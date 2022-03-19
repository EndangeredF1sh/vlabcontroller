package hk.edu.polyu.comp.vlabcontroller.backend.kubernetes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import hk.edu.polyu.comp.vlabcontroller.VLabControllerException;
import hk.edu.polyu.comp.vlabcontroller.backend.AbstractContainerBackend;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ContainerGroup;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ContainerSpec;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.spec.expression.SpecExpressionContext;
import hk.edu.polyu.comp.vlabcontroller.util.RFC6335Validator;
import hk.edu.polyu.comp.vlabcontroller.util.Retrying;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.vavr.Function0;
import io.vavr.Function1;
import io.vavr.control.Try;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.inject.Inject;
import javax.json.JsonPatch;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vavr.API.unchecked;
import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;

@Slf4j
@RefreshScope
public class KubernetesBackend extends AbstractContainerBackend {
    private static final String PARAM_POD = "pod";
    private static final String PARAM_CONTAINER = "container";
    private static final String PARAM_SERVICE = "service";
    private static final String PARAM_PVC = "persistentvolumeclaim";
    private static final String PARAM_NAMESPACE = "namespace";

    private static final String SECRET_KEY_REF = "secretKeyRef";

    @Setter(onMethod_ = {@Inject})
    private Retrying retrying;

    @Setter(onMethod_ = {@Inject})
    private PodPatcher podPatcher;

    @Setter(onMethod_ = {@Inject})
    private ProxyService proxyService;

    @Setter(onMethod_ = {@Inject})
    private ObjectMapper objectMapper;

    @Setter(onMethod_ = {@Inject})
    private ThreadPoolTaskScheduler taskScheduler;

    private KubernetesClient kubeClient;

    private Optional<ScheduledFuture<?>> cleanupPodsFuture = Optional.empty();

    Function0<Runnable> cleanupPodsTask = () -> () -> Optional.ofNullable(getFailedAndUnknownPods())
        .map(PodList::getItems)
        .filter(Predicate.not(List::isEmpty))
        .stream()
        .flatMap(Collection::stream)
        .map(Pod::getMetadata)
        .map(ObjectMeta::getLabels)
        .map(x -> x.get("comp.polyu.edu.hk/vl-proxy-id"))
        .forEach(proxyId -> {
            proxyService.stopProxy(proxyService.getProxy(proxyId), true, true, Duration.ZERO);
            log.error("Cleaned error proxy {}", proxyId);
        });

    @EventListener
    public void onRefreshScopeRefreshed(final RefreshScopeRefreshedEvent event) {
        log.debug("backend refreshed");
    }

    @Override
    public void initialize() throws VLabControllerException {
        super.initialize();

        var configBuilder = new ConfigBuilder();

        Optional.ofNullable(proxyProperties.getKubernetes().getUrl()).map(configBuilder::withMasterUrl);
        attachTLSCerts(configBuilder);
        kubeClient = new DefaultKubernetesClient(configBuilder.build());

        cleanBeforeStart();

        log.info("Enable failed and unknown phase pods detection & cleaning");
        startCleanupPods();
    }

    private void attachTLSCerts(ConfigBuilder configBuilder) {
        var certPath = proxyProperties.getKubernetes().getCertPath();
        Function<String, Optional<String>> loadFile = file -> Optional.of(Paths.get(certPath, file)).filter(Files::exists).map(Object::toString);
        if (certPath != null && Files.isDirectory(Paths.get(certPath))) {
            loadFile.apply("ca.pem").ifPresent(configBuilder::withCaCertFile);
            loadFile.apply("cert.pem").ifPresent(configBuilder::withClientCertFile);
            loadFile.apply("key.pem").ifPresent(configBuilder::withClientKeyFile);
        }
    }

    public void initialize(KubernetesClient client) {
        super.initialize();
        kubeClient = client;
        startCleanupPods();
    }

    void startCleanupPods() {
        cleanupPodsFuture.ifPresent(x -> x.cancel(true));
        cleanupPodsFuture = Optional.of(taskScheduler.scheduleAtFixedRate(cleanupPodsTask.apply(), Duration.ofSeconds(30)));
    }

    <T> Stream<T> evalExpressionForConfig(List<T> data, Class<T> type, SpecExpressionContext context) {
        Function1<T, T> evalExpressionOnConfig = x -> unchecked(objectMapper::writeValueAsString)
            .andThen(y -> expressionResolver.evaluateToString(y, context))
            .andThen(unchecked(y -> objectMapper.readValue(y, type)))
            .apply(x);

        return data.stream()
            .map(x -> Try.of(() -> evalExpressionOnConfig.apply(x)))
            .peek(x -> x.onFailure(e -> log.error("an error occured: {}", e)))
            .filter(Predicate.not(Try::isFailure))
            .map(Try::get);
    }

    @Override
    protected ContainerGroup startContainer(List<ContainerSpec> specs, Proxy proxy) throws Exception {
        var containerGroup = new ContainerGroup(UUID.randomUUID().toString(), new ArrayList<>(), new HashMap<>());

        var kubernetes = proxyProperties.getKubernetes();
        var kubeNamespace = kubernetes.getNamespace();
        var namespacePrefix = kubernetes.getNamespacePrefix();
        var uidNamespace = kubernetes.isCustomNamespace();
        log.debug("UserID Namespace Mode: {}", uidNamespace);
        if (uidNamespace) {
            kubeNamespace = Strings.isNullOrEmpty(namespacePrefix) ? proxy.getUserId() : String.format("%s-%s", namespacePrefix, proxy.getUserId());
        }
        proxy.setNamespace(kubeNamespace);

        var imagePullSecrets = Optional.ofNullable(kubernetes.getImagePullSecret())
            .map(List::of)
            .orElseGet(kubernetes::getImagePullSecrets)
            .stream().map(LocalObjectReference::new).collect(Collectors.toList());

        log.debug("imagePullSecrets: {}", imagePullSecrets);

        var specLabels = proxy.getSpec().getLabels();

        log.debug("all labels (map): {}", specLabels);

        // Handle runtime labels
        var runtimeLabels = specs.stream()
            .flatMap(x -> x.getRuntimeLabels().entrySet().stream())
            .filter(p -> p.getValue().getFirst())
            .collect(Collectors.toMap(Map.Entry::getKey, m -> m.getValue().getSecond(), (v1, v2) -> v2));

        var runtimeAnnotations = specs.stream()
            .flatMap(x -> x.getRuntimeLabels().entrySet().stream())
            .filter(p -> !p.getValue().getFirst())
            .collect(Collectors.toMap(Map.Entry::getKey, m -> m.getValue().getSecond(), (v1, v2) -> v2));

        var identifierLabel = proxyProperties.getIdentifierLabel();
        var identifierValue = proxyProperties.getIdentifierValue();
        var objectMetaBuilder = new ObjectMetaBuilder()
            .withNamespace(kubeNamespace)
            .withName("vl-pod-" + containerGroup.getId())
            .addToLabels(specLabels)
            .addToLabels(identifierLabel, identifierValue)
            .addToLabels(runtimeLabels)
            .addToAnnotations(runtimeAnnotations);

        var podBuilder = new PodBuilder()
            .withApiVersion(kubernetes.getApiVersion())
            .withKind("Pod")
            .withMetadata(objectMetaBuilder.build());

        var containers = specs.stream()
            .map(unchecked(spec -> {
                var volumeMounts = spec.getVolumeMounts();
                if (proxy.isAdmin()) {
                    var adminVolumeMounts = spec.getAdminVolumeMounts();
                    if (!adminVolumeMounts.isEmpty()) {
                        volumeMounts.addAll(adminVolumeMounts);
                        log.debug("Admin VolumeMount loaded: {}", adminVolumeMounts);
                    }
                }
                var envVars = buildEnv(spec, proxy).entrySet().stream()
                    .map(e -> {
                        var value = e.getValue();
                        if (startsWithIgnoreCase(value, SECRET_KEY_REF)) {
                            var ref = Pattern.compile(String.format("%s:(?<name>\\S+?):(?<key>\\S+)", SECRET_KEY_REF)).matcher(value);
                            if (!ref.matches()) {
                                log.warn(String.format("Invalid secret key reference: %s. Expected format: '%s:<name>:<key>'", e, SECRET_KEY_REF));
                                return Optional.<EnvVar>empty();
                            }
                            return Optional.of(new EnvVar(e.getKey(), null, new EnvVarSourceBuilder()
                                .withSecretKeyRef(new SecretKeySelectorBuilder()
                                    .withName(ref.group("name"))
                                    .withKey(ref.group("key"))
                                    .build()).build()));
                        }

                        return Optional.of(new EnvVar(e.getKey(), value, null));
                    })
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

                var security = new SecurityContextBuilder()
                    .withPrivileged(kubernetes.isPrivileged() || spec.isPrivileged())
                    .build();

                var resources = spec.getResources();
                var containerBuilder = new ContainerBuilder()
                    .withImage(spec.getImage())
                    .withCommand(spec.getCmd())
                    .withName(
                        Optional.ofNullable(spec.getName()).filter(Predicate.not(String::isBlank))
                            .orElse(String.format("vl-container-%s", UUID.randomUUID()))
                    )
                    .withPorts(
                        spec.getPortMapping().entrySet().stream()
                            .map(e -> {
                                var builder = new ContainerPortBuilder();
                                if (RFC6335Validator.valid(e.getKey())) builder = builder.withName(e.getKey());
                                builder = builder.withContainerPort(e.getValue());
                                return builder.build();
                            })
                            .collect(Collectors.toList())
                    )
                    .withVolumeMounts(volumeMounts)
                    .withSecurityContext(security)
                    .withResources(resources.asResourceRequirements())
                    .withEnv(envVars);

                var imagePullPolicy = kubernetes.getImagePullPolicy();
                if (imagePullPolicy != null) containerBuilder.withImagePullPolicy(imagePullPolicy);

                return containerBuilder.build();
            }))
            .collect(Collectors.toList());

        containerGroup.getParameters().put(PARAM_CONTAINER, containers);
        var context = SpecExpressionContext.create(proxy, proxy.getSpec());
        var volumes = evalExpressionForConfig(proxy.getSpec().getKubernetes().getVolumes(), Volume.class, context)
            .collect(Collectors.toList());
        log.debug("containers created: {}", containers.size());
        log.debug("volumes created: {}", volumes.size());

        var podSpec = new PodSpec();
        podSpec.setContainers(containers);
        podSpec.setVolumes(volumes);
        podSpec.setImagePullSecrets(imagePullSecrets);

        if (proxy.getSpec().isSecure()) {
            podSpec.setRuntimeClassName(proxyProperties.getKubernetes().getSecureRuntimeName());
        }

        var nodeSelectorString = kubernetes.getNodeSelector();
        if (nodeSelectorString != null) {
            podSpec.setNodeSelector(Splitter.on(",").withKeyValueSeparator("=").split(nodeSelectorString));
        }

        log.debug("nodeSelectorString: {}", nodeSelectorString);

        var patchedPod = podPatcher.patchWithDebug(podBuilder.withSpec(podSpec).build(), readPatchFromSpec(proxy));
        final var effectiveKubeNamespace = patchedPod.getMetadata().getNamespace(); // use the namespace of the patched Pod, in case the patch changes the namespace.
        containerGroup.getParameters().put(PARAM_NAMESPACE, effectiveKubeNamespace);

        var pvcs = evalExpressionForConfig(proxy.getSpec().getKubernetes().getPersistentVolumeClaims(), PersistentVolumeClaim.class, context)
            .peek(expressionPVC -> {
                var labelCache = Optional.ofNullable(expressionPVC.getMetadata().getLabels()).orElseGet(HashMap::new);
                labelCache.putAll(specLabels);
                labelCache.putAll(runtimeLabels);
                labelCache.put(identifierLabel, identifierValue);
                expressionPVC.getMetadata().setLabels(labelCache);
            })
            .map(expressionPVC -> kubeClient.persistentVolumeClaims().inNamespace(effectiveKubeNamespace).createOrReplace(expressionPVC))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        log.debug("created {} PVCs", pvcs.size());
        containerGroup.getParameters().put(PARAM_PVC, pvcs);

        // create additional manifests -> use the effective (i.e. patched) namespace if no namespace is provided
        createAdditionalManifests(proxy, effectiveKubeNamespace, specLabels, runtimeLabels);

        var startedPod = kubeClient
            .pods()
            .inNamespace(effectiveKubeNamespace)
            .create(patchedPod);

        log.debug("pod started");

        var pod = waitUntilPodReadyOrDie(startedPod);
        containerGroup.getParameters().put(PARAM_POD, pod);
        log.debug("pod registered");

        // If SP runs inside the cluster, it can access pods directly and doesn't need any port publishing service.
        if (!proxyProperties.getKubernetes().isInternalNetworking()) {
            var ports = specs.stream()
                .flatMap(x -> x.getPortMapping().entrySet().stream())
                .map(e -> {
                    var builder = new ServicePortBuilder();
                    if (RFC6335Validator.valid(e.getKey())) builder = builder.withName(e.getKey()).withNewTargetPort(e.getKey());
                    else builder = builder.withNewTargetPort(e.getValue());
                    return builder.build();
                })
                .collect(Collectors.toList());
            var service = startService(effectiveKubeNamespace, new ServiceBuilder()
                .withApiVersion(kubernetes.getApiVersion())
                .withKind("Service")
                .withNewMetadata()
                .withName("vl-service-" + containerGroup.getId())
                .addToLabels(identifierLabel, identifierValue)
                .addToLabels(specLabels)
                .addToLabels(runtimeLabels)
                .endMetadata()
                .withNewSpec()
                .addToSelector(RUNTIME_LABEL_PROXY_ID, proxy.getId())
                .withType("NodePort")
                .withPorts(ports)
                .endSpec()
                .build()
            );
            containerGroup.getParameters().put(PARAM_SERVICE, service);
            log.debug("service registered");
            calculateProxyRoutes(specs, proxy, containerGroup, service);
        }

        specs.stream()
            .filter(spec -> parseBoolean(spec.getRuntimeLabels().get(RUNTIME_LABEL_EVALUATOR).getSecond()))
            .findAny()
            .ifPresent(evaluatorSpec -> startService(effectiveKubeNamespace, new ServiceBuilder()
                .withApiVersion(kubernetes.getApiVersion())
                .withKind("Service")
                .withNewMetadata()
                .addToLabels(identifierLabel, identifierValue)
                .addToLabels(specLabels)
                .addToLabels(runtimeLabels)
                .withName("vl-evaluator-" + containerGroup.getId())
                .endMetadata()
                .withNewSpec()
                .addToSelector(RUNTIME_LABEL_PROXY_ID, proxy.getId())
                .withType("ClusterIP")
                .withPorts(List.of(new ServicePortBuilder().withName("rpc").withPort(80).withNewTargetPort("rpc").build()))
                .endSpec()
                .build()
            ));


        return containerGroup;
    }

    private void createAdditionalManifests(Proxy proxy, String namespace, Map<String, String> specLabels, Map<String, String> runtimeLabels) {
        getAdditionManifestsAsObjects(proxy, namespace).stream()
            .filter(fullObject -> kubeClient.resource(fullObject).fromServer().get() == null)
            .forEach(fullObject -> {
                var identifierLabel = proxyProperties.getIdentifierLabel();
                var identifierValue = proxyProperties.getIdentifierValue();
                var cache = fullObject.getMetadata();
                var labels = Optional.ofNullable(cache.getLabels()).orElseGet(HashMap::new);
                labels.put(identifierLabel, identifierValue);
                labels.putAll(specLabels);
                labels.putAll(runtimeLabels);
                cache.setLabels(labels);
                fullObject.setMetadata(cache);
                kubeClient.resource(fullObject).createOrReplace();
            });
    }

    private JsonPatch readPatchFromSpec(Proxy proxy) throws JsonProcessingException {
        var patchAsString = proxy.getSpec().getKubernetes().getPodPatches();
        if (patchAsString == null) {
            return null;
        }

        // resolve expressions
        var context = SpecExpressionContext.create(proxy, proxy.getSpec());
        var expressionAwarePatch = expressionResolver.evaluateToString(patchAsString, context);

        var yamlReader = new ObjectMapper(new YAMLFactory());
        yamlReader.registerModule(new JSR353Module());
        return yamlReader.readValue(expressionAwarePatch, JsonPatch.class);
    }

    private Pod waitUntilPodReadyOrDie(Pod startedPod) throws ExecutionException, InterruptedException {
        var maxTries = (int) proxyProperties.getKubernetes().getPodWaitTime().toSeconds();
        var retry = retrying.retry(i -> {
            if (Readiness.isPodReady(kubeClient.resource(startedPod).fromServer().get())) return true;
            if (i > 1) log.debug(String.format("Container not ready yet, trying again (%d/%d)", i, maxTries));
            return false;
        }, maxTries, Duration.ofSeconds(1));
        if (retry.get()) return kubeClient.resource(startedPod).fromServer().get();
        throw new VLabControllerException("Container did not become ready in time");
    }

    // Calculate proxy routes for all configured ports.
    private void calculateProxyRoutes(List<ContainerSpec> specs, Proxy proxy, ContainerGroup containerGroup, Service service) {
        var targetMaps = specs.stream()
            .flatMap(x -> x.getPortMapping().entrySet().stream())
            .collect(Collectors.toMap(
                entry -> mappingStrategy.createMapping(entry.getKey(), containerGroup, proxy),
                unchecked(entry -> calculateTarget(containerGroup, entry.getValue(),
                    Optional.ofNullable(service).flatMap(x ->
                        x.getSpec().getPorts().stream()
                            .filter(p -> p.getPort().equals(entry.getValue()))
                            .map(ServicePort::getNodePort)
                            .findAny()
                    ).orElse(-1)
                ))));
        log.debug("adding target maps: {}", targetMaps);
        proxy.getTargets().putAll(targetMaps);
    }

    private Service startService(String effectiveKubeNamespace, Service startupService) {
        kubeClient.services().inNamespace(effectiveKubeNamespace).createOrReplace(startupService);
        retrying.retry(i -> isServiceReady(kubeClient.resource(startupService).fromServer().get()), 60, Duration.ofSeconds(1));
        return kubeClient.resource(startupService).fromServer().get();
    }


    /**
     * Converts the additional manifests of the spec into HasMetadata objects.
     * When the resource has no namespace definition, the provided namespace
     * parameter will be used.
     */
    private List<HasMetadata> getAdditionManifestsAsObjects(Proxy proxy, String namespace) {
        var context = SpecExpressionContext.create(proxy, proxy.getSpec());
        return proxy.getSpec().getKubernetes().getAdditionalManifests().stream()
            .map(manifest -> expressionResolver.evaluateToString(manifest, context).getBytes())
            .map(bs -> {
                HasMetadata object = Serialization.unmarshal(new ByteArrayInputStream(bs)); // used to determine whether the manifest has specified a namespace
                var fullObject = kubeClient.load(new ByteArrayInputStream(bs)).get().get(0);
                if (object.getMetadata().getNamespace() == null) {
                    // the load method (in some cases) automatically sets a namespace when no namespace is provided
                    // therefore we overwrite this namespace with the namespace of the pod.
                    fullObject.getMetadata().setNamespace(namespace);
                }
                return fullObject;
            }).collect(Collectors.toList());
    }

    private boolean isServiceReady(Service service) {
        return Optional.ofNullable(service).map(Service::getStatus)
            .map(ServiceStatus::getLoadBalancer).isPresent();
    }

    protected URI calculateTarget(ContainerGroup containerGroup, int containerPort, int servicePort) throws Exception {
        var kubernetes = proxyProperties.getKubernetes();
        var targetProtocol = kubernetes.getContainerProtocol();
        String targetHostName;
        int targetPort;

        var pod = (Pod) containerGroup.getParameters().get(PARAM_POD);

        if (kubernetes.isInternalNetworking()) {
            targetHostName = pod.getStatus().getPodIP();
            targetPort = containerPort;
        } else {
            targetHostName = pod.getStatus().getHostIP();
            targetPort = servicePort;
        }

        return new URI(String.format("%s://%s:%s", targetProtocol, targetHostName, targetPort));
    }

    @Override
    protected void doStopProxy(Proxy proxy) throws VLabControllerException {
        var kubeNamespace = proxy.getNamespace();
        if (kubeNamespace == null) {
            throw new VLabControllerException("Failed to stop proxy: Cannot get proxy's namespace");
        }

        kubeClient.pods().inNamespace(kubeNamespace).withLabel(RUNTIME_LABEL_PROXY_ID, proxy.getId()).delete();
        kubeClient.services().inNamespace(kubeNamespace).withLabel(RUNTIME_LABEL_PROXY_ID, proxy.getId()).delete();
        kubeClient.persistentVolumeClaims().inNamespace(kubeNamespace).withLabel(RUNTIME_LABEL_PROXY_ID, proxy.getId()).delete();

        // delete additional manifests
        getAdditionManifestsAsObjects(proxy, kubeNamespace).forEach(fullObject -> kubeClient.resource(fullObject).delete());
    }

    @Override
    public BiConsumer<OutputStream, OutputStream> getOutputAttacher(Proxy proxy) {
        var containerGroup = proxy.getContainerGroup();
        var parameters = containerGroup.getParameters();
        var containers = (List<Container>) parameters.get(PARAM_CONTAINER);
        if (containers.isEmpty()) return null;
        return (stdOut, stdErr) -> {
            try {
                var kubeNamespace = Optional.ofNullable(parameters.get(PARAM_NAMESPACE).toString())
                    .orElseGet(() -> proxyProperties.getKubernetes().getNamespace());
                try (var watcher = kubeClient.pods().inNamespace(kubeNamespace).withName("vl-pod-" + containerGroup.getId()).watchLog()) {
                    watcher.getOutput().transferTo(stdOut);
                }
            } catch (IOException e) {
                log.error("Error while attaching to container output", e);
            }
        };
    }

    public void cleanBeforeStart() {
        var identifierLabel = proxyProperties.getIdentifierLabel();
        var identifierValue = proxyProperties.getIdentifierValue();
        Optional.ofNullable(kubeClient.pods().inAnyNamespace().withLabel(identifierLabel, identifierValue).list())
            .ifPresent(orphanPods -> {
                orphanPods.getItems().forEach(pod -> {
                    var namespace = pod.getMetadata().getNamespace();
                    kubeClient.pods().inNamespace(namespace).delete(pod);
                });
                log.info("Cleaned {} pods", orphanPods.getItems().size());
            });

        Optional.ofNullable(kubeClient.services().inAnyNamespace().withLabel(identifierLabel, identifierValue).list())
            .ifPresent(orphanServices -> {
                orphanServices.getItems().forEach(service -> {
                    var namespace = service.getMetadata().getNamespace();
                    kubeClient.services().inNamespace(namespace).delete(service);
                });
                log.info("Cleaned {} services", orphanServices.getItems().size());
            });

        Optional.ofNullable(kubeClient.persistentVolumeClaims().inAnyNamespace().withLabel(identifierLabel, identifierValue).list())
            .ifPresent(orphanPVCs -> {
                orphanPVCs.getItems().forEach(pvc -> {
                    var namespace = pvc.getMetadata().getNamespace();
                    kubeClient.persistentVolumeClaims().inNamespace(namespace).delete(pvc);
                });
                log.info("Cleaned {} PersistentVolumeClaims", orphanPVCs.getItems().size());
            });
    }

    public PodList getFailedAndUnknownPods() {
        var identifierLabel = proxyProperties.getIdentifierLabel();
        var identifierValue = proxyProperties.getIdentifierValue();
        return kubeClient.pods().inAnyNamespace()
            .withLabel(identifierLabel, identifierValue)
            .withoutField("status.phase", "Pending")
            .withoutField("status.phase", "Running")
            .withoutField("status.phase", "Succeeded")
            .list();
    }
}