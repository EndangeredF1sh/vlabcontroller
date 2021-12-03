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
import hk.edu.polyu.comp.vlabcontroller.util.Retrying;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.kubernetes.client.utils.Serialization;
import javax.json.JsonPatch;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.pivovarit.function.ThrowingFunction.unchecked;

public class KubernetesBackend extends AbstractContainerBackend {

    private static final String PROPERTY_PREFIX = "proxy.kubernetes.";

    private static final String PROPERTY_NAMESPACE = "namespace";
    private static final String PROPERTY_API_VERSION = "api-version";
    private static final String PROPERTY_IMG_PULL_POLICY = "image-pull-policy";
    private static final String PROPERTY_IMG_PULL_SECRETS = "image-pull-secrets";
    private static final String PROPERTY_IMG_PULL_SECRET = "image-pull-secret";
    private static final String PROPERTY_NODE_SELECTOR = "node-selector";
    private static final String PROPERTY_UID_NAMESPACE = "custom-namespace";
    private static final String PROPERTY_NAMESPACE_PREFIX = "namespace-prefix";

    private static final String DEFAULT_NAMESPACE = "default";
    private static final String DEFAULT_API_VERSION = "v1";

    private static final String PARAM_POD = "pod";
    private static final String PARAM_CONTAINER = "container";
    private static final String PARAM_SERVICE = "service";
    private static final String PARAM_PVC = "persistentvolumeclaim";
    private static final String PARAM_NAMESPACE = "namespace";

    private static final String SECRET_KEY_REF = "secretKeyRef";

    private final Logger log = LogManager.getLogger(KubernetesBackend.class);

    @Inject
    private PodPatcher podPatcher;

    @Inject
    private ProxyService proxyService;

    @Inject
    private ObjectMapper objectMapper;

    private KubernetesClient kubeClient;

    @Override
    public void initialize() throws VLabControllerException {
        super.initialize();

        var configBuilder = new ConfigBuilder();

        var masterUrl = getProperty(PROPERTY_URL);
        if (masterUrl != null) configBuilder.withMasterUrl(masterUrl);
        attachTLSCerts(configBuilder);
        kubeClient = new DefaultKubernetesClient(configBuilder.build());

        cleanBeforeStart();

        var cleanFailedThread = new Thread(new ErrorPodsCleaner(), ErrorPodsCleaner.class.getSimpleName());
        cleanFailedThread.setDaemon(true);
        cleanFailedThread.start();
    }

    private void attachTLSCerts(ConfigBuilder configBuilder) {
        var certPath = getProperty(PROPERTY_CERT_PATH);
        if (certPath != null && Files.isDirectory(Paths.get(certPath))) {
            var certFilePath = Paths.get(certPath, "ca.pem");
            if (Files.exists(certFilePath)) configBuilder.withCaCertFile(certFilePath.toString());
            certFilePath = Paths.get(certPath, "cert.pem");
            if (Files.exists(certFilePath)) configBuilder.withClientCertFile(certFilePath.toString());
            certFilePath = Paths.get(certPath, "key.pem");
            if (Files.exists(certFilePath)) configBuilder.withClientKeyFile(certFilePath.toString());
        }
    }

    public void initialize(KubernetesClient client) {
        super.initialize();
        kubeClient = client;
        var cleanFailedThread = new Thread(new ErrorPodsCleaner(), ErrorPodsCleaner.class.getSimpleName());
        cleanFailedThread.setDaemon(true);
        cleanFailedThread.start();
    }

    @Override
    protected ContainerGroup startContainer(List<ContainerSpec> specs, Proxy proxy) throws Exception {
        var containerGroup = new ContainerGroup();
        containerGroup.setSpecs(specs);
        containerGroup.setId(UUID.randomUUID().toString());

        var identifierLabel = environment.getProperty("proxy.identifier-label", "comp.polyu.edu.hk/vl-identifier");
        var identifierValue = environment.getProperty("proxy.identifier-value", "default-identifier");

        var kubeNamespace = getProperty(PROPERTY_NAMESPACE, DEFAULT_NAMESPACE);
        var namespacePrefix = getProperty(PROPERTY_NAMESPACE_PREFIX);
        var uidNamespace = Boolean.parseBoolean(getProperty(PROPERTY_UID_NAMESPACE, "false"));
        log.debug("UserID Namespace Mode: {}", uidNamespace);
        if (uidNamespace) {
            kubeNamespace = Strings.isNullOrEmpty(namespacePrefix) ? proxy.getUserId() : String.format("%s-%s", namespacePrefix, proxy.getUserId());
        }
        proxy.setNamespace(kubeNamespace);

        var apiVersion = getProperty(PROPERTY_API_VERSION, DEFAULT_API_VERSION);

        var imagePullSecrets = Optional.ofNullable(getProperty(PROPERTY_IMG_PULL_SECRET))
                .map(List::of)
                .or(() -> Optional.ofNullable(getProperty(PROPERTY_IMG_PULL_SECRETS)).map(x -> x.split(",")).map(List::of))
                .orElse(List.of())
                .stream().map(LocalObjectReference::new).collect(Collectors.toList());

        log.debug("imagePullSecrets: {}", imagePullSecrets);

        var labels = proxy.getSpec().getLabels();

        log.debug("all labels (map): {}", labels);

        var objectMetaBuilder = new ObjectMetaBuilder()
                .withNamespace(kubeNamespace)
                .withName("sp-pod-" + containerGroup.getId())
                .addToLabels(labels)
                .addToLabels(identifierLabel, identifierValue)
                .addToLabels("app", containerGroup.getId());

        // Handle runtime labels
        specs.stream()
                .flatMap(x -> x.getRuntimeLabels().entrySet().stream())
                .forEach(runtimeLabel -> {
                    if (runtimeLabel.getValue().getFirst()) {
                        objectMetaBuilder.addToLabels(runtimeLabel.getKey(), runtimeLabel.getValue().getSecond());
                    } else {
                        objectMetaBuilder.addToAnnotations(runtimeLabel.getKey(), runtimeLabel.getValue().getSecond());
                    }
                });

        var podBuilder = new PodBuilder()
            .withApiVersion(apiVersion)
            .withKind("Pod")
            .withMetadata(objectMetaBuilder.build());

        var containers = specs.stream()
                .map(unchecked(spec -> {
                    var volumeMounts = spec.getVolumeMount();
                    var envVars = buildEnv(spec, proxy).stream()
                            .map(envString -> {
                                var e = envString.split("=");
                                if (e.length == 1) e = new String[]{e[0], ""};
                                if (e.length > 2) e[1] = envString.substring(envString.indexOf('=') + 1);
                                if (!e[1].toLowerCase().startsWith(SECRET_KEY_REF.toLowerCase())) {
                                    return Optional.of(new EnvVar(e[0], e[1], null));
                                }
                                var ref = e[1].split(":");
                                if (ref.length != 3) {
                                    log.warn(String.format("Invalid secret key reference: %s. Expected format: '%s:<name>:<key>'", envString, SECRET_KEY_REF));
                                    return Optional.<EnvVar>empty();
                                }
                                var secretKeyRef = new SecretKeySelectorBuilder()
                                        .withName(ref[1])
                                        .withKey(ref[2])
                                        .build();
                                var envVarSourceBuilder = new EnvVarSourceBuilder()
                                        .withSecretKeyRef(secretKeyRef);
                                return Optional.of(new EnvVar(e[0], null, envVarSourceBuilder.build()));
                            })
                            .flatMap(Optional::stream)
                            .collect(Collectors.toList());

                    var security = new SecurityContextBuilder()
                            .withPrivileged(isPrivileged() || spec.isPrivileged())
                            .build();

                    var toQuantity = (Function<String, Quantity>) (String x) -> Optional.ofNullable(x).map(Quantity::new).orElse(null);
                    var containerBuilder = new ContainerBuilder()
                            .withImage(spec.getImage())
                            .withCommand(spec.getCmd())
                            .withName(String.format("sp-container-%s", UUID.randomUUID()))
                            .withPorts(
                                    spec.getPortMapping().values().stream()
                                            .map(p -> new ContainerPortBuilder().withContainerPort(p).build())
                                            .collect(Collectors.toList())
                            )
                            .withVolumeMounts(volumeMounts)
                            .withSecurityContext(security)
                            .withResources(
                                    new ResourceRequirementsBuilder()
                                        .addToRequests(spec.getResources().getRequests().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> toQuantity.apply(v.getValue()))))
                                        .addToLimits(spec.getResources().getLimits().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> toQuantity.apply(v.getValue()))))
                                        .build()
                            )
                            .withEnv(envVars);

                    var imagePullPolicy = getProperty(PROPERTY_IMG_PULL_POLICY);
                    if (imagePullPolicy != null) containerBuilder.withImagePullPolicy(imagePullPolicy);

                    return containerBuilder.build();
                }))
                .collect(Collectors.toList());
        containerGroup.getParameters().put(PARAM_CONTAINER, containers);
        SpecExpressionContext context = SpecExpressionContext.create(proxy, proxy.getSpec());
        List<Volume> volumes = proxy.getSpec().getKubernetes().getVolumes().stream().map(volume -> {
            try {
                String volumeString = objectMapper.writeValueAsString(volume);
                volumeString = expressionResolver.evaluateToString(volumeString, context);
                return objectMapper.readValue(volumeString, Volume.class);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }).collect(Collectors.toList());

        log.debug("containers created: {}", containers.size());
        log.debug("volumes created: {}", volumes.size());

        var podSpec = new PodSpec();
        podSpec.setContainers(containers);
        podSpec.setVolumes(volumes);
        podSpec.setImagePullSecrets(imagePullSecrets);

        var nodeSelectorString = getProperty(PROPERTY_NODE_SELECTOR);
        if (nodeSelectorString != null) {
            podSpec.setNodeSelector(Splitter.on(",").withKeyValueSeparator("=").split(nodeSelectorString));
        }

        log.debug("nodeSelectorString: {}", nodeSelectorString);

        var startupPod = podBuilder
                .withSpec(podSpec)
                .build();

        JsonPatch patch = readPatchFromSpec(proxy);
        Pod patchedPod = podPatcher.patchWithDebug(startupPod, patch);
        final String effectiveKubeNamespace = patchedPod.getMetadata().getNamespace(); // use the namespace of the patched Pod, in case the patch changes the namespace.
        containerGroup.getParameters().put(PARAM_NAMESPACE, effectiveKubeNamespace);

        var pvcs = proxy.getSpec().getKubernetes().getPersistentVolumeClaims().stream().map(pvc -> {
            try {
                String pvcString = objectMapper.writeValueAsString(pvc);
                pvcString = expressionResolver.evaluateToString(pvcString, context);
                var expressionPVC = objectMapper.readValue(pvcString, PersistentVolumeClaim.class);
                return kubeClient.persistentVolumeClaims().inNamespace(effectiveKubeNamespace).createOrReplace(expressionPVC);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }).collect(Collectors.toList());
        containerGroup.getParameters().put(PARAM_PVC, pvcs.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        log.debug("created {} PVCs", pvcs.stream().filter(Objects::nonNull).count());

        // create additional manifests -> use the effective (i.e. patched) namespace if no namespace is provided
        createAdditionalManifests(proxy, effectiveKubeNamespace);

        var startedPod = kubeClient
                .pods()
                .inNamespace(effectiveKubeNamespace)
                .create(patchedPod);

        log.debug("pod started");

        var pod = waitUntilPodReadyOrDie(startedPod);
        containerGroup.getParameters().put(PARAM_POD, pod);
        log.debug("pod registered");

        // If SP runs inside the cluster, it can access pods directly and doesn't need any port publishing service.
        var service = makeServiceIfNecessary(specs, proxy, containerGroup, identifierLabel, identifierValue, apiVersion, labels, effectiveKubeNamespace);
        containerGroup.getParameters().put(PARAM_SERVICE, service);

        log.debug("service registered");

        calculateProxyRoutes(specs, proxy, containerGroup, service);

        return containerGroup;
    }

    private void createAdditionalManifests(Proxy proxy, String namespace) {
        for (HasMetadata fullObject : getAdditionManifestsAsObjects(proxy, namespace)) {
            if (kubeClient.resource(fullObject).fromServer().get() == null) {
                String identifierLabel = environment.getProperty("proxy.identifier-label", "comp.polyu.edu.hk/vl-identifier");
                String identifierValue = environment.getProperty("proxy.identifier-value", "default-identifier");
                ObjectMeta cache = fullObject.getMetadata();
                Map<String, String> labels = cache.getLabels();
                if (labels == null) {
                    labels = new HashMap<>();
                }
                labels.put(identifierLabel, identifierValue);
                cache.setLabels(labels);
                fullObject.setMetadata(cache);
                kubeClient.resource(fullObject).createOrReplace();
            }
        }
    }

    private JsonPatch readPatchFromSpec(Proxy proxy) throws JsonProcessingException {
        String patchAsString = proxy.getSpec().getKubernetes().getPodPatches();
        if (patchAsString == null) {
            return null;
        }

        // resolve expressions
        SpecExpressionContext context = SpecExpressionContext.create(proxy, proxy.getSpec());
        String expressionAwarePatch = expressionResolver.evaluateToString(patchAsString, context);

        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        yamlReader.registerModule(new JSR353Module());
        return yamlReader.readValue(expressionAwarePatch, JsonPatch.class);
    }


    private Pod waitUntilPodReadyOrDie(Pod startedPod) {
        var totalWaitMs = Integer.parseInt(environment.getProperty("proxy.kubernetes.pod-wait-time", "60000"));
        var maxTries = totalWaitMs / 1000;
        boolean result = Retrying.retry(i -> {
            var pod = kubeClient.resource(startedPod).fromServer().get();
            if (!Readiness.isPodReady(pod)) {
                if (i > 1)
                    log.debug(String.format("Container not ready yet, trying again (%d/%d)", i, maxTries));
                return false;
            }
            return true;
        }, maxTries, 1000);
        if (!result){
            throw new VLabControllerException("Container did not become ready in time");
        }
        return kubeClient.resource(startedPod).fromServer().get();
    }

    // Calculate proxy routes for all configured ports.
    private void calculateProxyRoutes(List<ContainerSpec> specs, Proxy proxy, ContainerGroup containerGroup, Service service) throws Exception {
        for (var entry : specs.stream()
                .flatMap(x -> x.getPortMapping().entrySet().stream())
                .collect(Collectors.toList())) {
            var servicePort = service == null ? -1 : service.getSpec().getPorts().stream()
                    .filter(p -> p.getPort().equals(entry.getValue())).map(ServicePort::getNodePort)
                    .findAny().orElse(-1);

            var mapping = mappingStrategy.createMapping(entry.getKey(), containerGroup, proxy);
            var target = calculateTarget(containerGroup, entry.getValue(), servicePort);
            log.debug("adding {} to {}", target, mapping);
            proxy.getTargets().put(mapping, target);
        }
    }

    private Service makeServiceIfNecessary(List<ContainerSpec> specs, Proxy proxy, ContainerGroup containerGroup, String identifierLabel, String identifierValue, String apiVersion, Map<String, String> allLabels, String effectiveKubeNamespace) {
        Service service = null;
        if (!isUseInternalNetwork()) {
            var servicePorts = specs.stream()
                    .flatMap(x -> x.getPortMapping().values().stream())
                    .map(p -> new ServicePortBuilder().withPort(p).build())
                    .collect(Collectors.toList());

            var startupService = new ServiceBuilder()
                    .withApiVersion(apiVersion)
                    .withKind("Service")
                    .withNewMetadata()
                    .withName("sp-service-" + containerGroup.getId())
                    .addToLabels(RUNTIME_LABEL_PROXY_ID, proxy.getId())
                    .addToLabels(RUNTIME_LABEL_PROXIED_APP, "true")
                    .addToLabels(RUNTIME_LABEL_INSTANCE, instanceId)
                    .addToLabels(identifierLabel, identifierValue)
                    .addToLabels(allLabels)
                    .endMetadata()
                    .withNewSpec()
                    .addToSelector("app", containerGroup.getId())
                    .withType("NodePort")
                    .withPorts(servicePorts)
                    .endSpec()
                    .build();
            kubeClient.services().inNamespace(effectiveKubeNamespace).createOrReplace(startupService);
            // Workaround: waitUntilReady appears to be buggy.
            Retrying.retry(i -> isServiceReady(kubeClient.resource(startupService).fromServer().get()), 60, 1000);

            service = kubeClient.resource(startupService).fromServer().get();
        }
        return service;
    }

    /**
     * Converts the additional manifests of the spec into HasMetadata objects.
     * When the resource has no namespace definition, the provided namespace
     * parameter will be used.
     */
    private List<HasMetadata> getAdditionManifestsAsObjects(Proxy proxy, String namespace) {
        var context = SpecExpressionContext.create(proxy, proxy.getSpec());

        var result = new ArrayList<HasMetadata>();
        for (var manifest : proxy.getSpec().getKubernetes().getAdditionalManifests()) {
            var expressionManifest = expressionResolver.evaluateToString(manifest, context);
            HasMetadata object = Serialization.unmarshal(new ByteArrayInputStream(expressionManifest.getBytes())); // used to determine whether the manifest has specified a namespace

            var fullObject = kubeClient.load(new ByteArrayInputStream(expressionManifest.getBytes())).get().get(0);
            if (object.getMetadata().getNamespace() == null) {
                // the load method (in some cases) automatically sets a namespace when no namespace is provided
                // therefore we overwrite this namespace with the namespace of the pod.
                fullObject.getMetadata().setNamespace(namespace);
            }
            result.add(fullObject);
        }
        return result;
    }

    private boolean isServiceReady(Service service) {
        if (service == null) {
            return false;
        }
        if (service.getStatus() == null) {
            return false;
        }
        return service.getStatus().getLoadBalancer() != null;
    }

    protected URI calculateTarget(ContainerGroup containerGroup, int containerPort, int servicePort) throws Exception {
        var targetProtocol = getProperty(PROPERTY_CONTAINER_PROTOCOL, DEFAULT_TARGET_PROTOCOL);
        String targetHostName;
        int targetPort;

        var pod = (Pod) containerGroup.getParameters().get(PARAM_POD);

        if (isUseInternalNetwork()) {
            targetHostName = pod.getStatus().getPodIP();
            targetPort = containerPort;
        } else {
            targetHostName = pod.getStatus().getHostIP();
            targetPort = servicePort;
        }

        return new URI(String.format("%s://%s:%s", targetProtocol, targetHostName, targetPort));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doStopProxy(Proxy proxy) throws Exception {
        // wait a bit to make sure there will be a pod to remove
        // prevent orphan pods issue (start an app and sign out quickly)
        var retry = 20;
        var interval = 3000;
        var responded = false;
        var containerGroup = proxy.getContainerGroup();
        for (var i = 0; i < retry; i++) {
            var containers = (List<Container>) containerGroup.getParameters().get(PARAM_CONTAINER);
            if (containers.isEmpty()) {
                log.debug("Proxy pod unresponsive, retrying ({}/{})", i + 1, retry);
                Thread.sleep(interval);
            } else {
                responded = true;
                log.debug("Pod responded, removing...");
                break;
            }
        }
        if (!responded) {
            var kubeNamespace = getProperty(PROPERTY_NAMESPACE, DEFAULT_NAMESPACE);
            var namespacePrefix = getProperty(PROPERTY_NAMESPACE_PREFIX);
            if (Boolean.parseBoolean(getProperty(PROPERTY_UID_NAMESPACE, "false"))) {
                kubeNamespace = Strings.isNullOrEmpty(namespacePrefix) ? proxy.getUserId() : namespacePrefix + "-" + proxy.getUserId();
            }
            // we have to delete additional manifests and services even though we can't find pod
            for (var fullObject : getAdditionManifestsAsObjects(proxy, kubeNamespace)) {
                kubeClient.resource(fullObject).delete();
            }
            kubeClient.services().inNamespace(kubeNamespace).withLabel(RUNTIME_LABEL_PROXY_ID, proxy.getId()).delete();
            throw new VLabControllerException("Failed to stop container: no pod was founded");
        }

        var kubeNamespace = containerGroup.getParameters().get(PARAM_NAMESPACE).toString();
        if (kubeNamespace == null) {
            kubeNamespace = getProperty(PROPERTY_NAMESPACE, DEFAULT_NAMESPACE);
        }

        var pod = (Pod) containerGroup.getParameters().get(PARAM_POD);
        if (pod != null) kubeClient.pods().inNamespace(kubeNamespace).delete(pod);
        var service = (Service) containerGroup.getParameters().get(PARAM_SERVICE);
        if (service != null) kubeClient.services().inNamespace(kubeNamespace).delete(service);
        var pvcs = (List<PersistentVolumeClaim>) containerGroup.getParameters().get(PARAM_PVC);
        if (!Objects.requireNonNull(pvcs).isEmpty()) kubeClient.persistentVolumeClaims().inNamespace(kubeNamespace).delete(pvcs);

        // delete additional manifests
        for (var fullObject : getAdditionManifestsAsObjects(proxy, kubeNamespace)) {
            kubeClient.resource(fullObject).delete();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public BiConsumer<OutputStream, OutputStream> getOutputAttacher(Proxy proxy) {
        var containerGroup = proxy.getContainerGroup();
        var containers = (List<Container>) containerGroup.getParameters().get(PARAM_CONTAINER);
        if (containers.isEmpty()) return null;
        return (stdOut, stdErr) -> {
            try {
                var kubeNamespace = containerGroup.getParameters().get(PARAM_NAMESPACE).toString();
                if (kubeNamespace == null) {
                    kubeNamespace = getProperty(PROPERTY_NAMESPACE, DEFAULT_NAMESPACE);
                }
                var watcher = kubeClient.pods().inNamespace(kubeNamespace).withName("sp-pod-" + containerGroup.getId()).watchLog();
                IOUtils.copy(watcher.getOutput(), stdOut);
            } catch (IOException e) {
                log.error("Error while attaching to container output", e);
            }
        };
    }

    @Override
    protected String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    public void cleanBeforeStart() {
        var identifierLabel = environment.getProperty("proxy.identifier-label", "comp.polyu.edu.hk/vl-identifier");
        var identifierValue = environment.getProperty("proxy.identifier-value", "default-identifier");
        var orphanPods = kubeClient.pods().inAnyNamespace().withLabel(identifierLabel, identifierValue).list();
        if (orphanPods != null) {
            for (var pod : orphanPods.getItems()) {
                var namespace = pod.getMetadata().getNamespace();
                kubeClient.pods().inNamespace(namespace).delete(pod);
            }
            log.info("Cleaned {} pods", orphanPods.getItems().size());
        }
        var orphanServices = kubeClient.services().inAnyNamespace().withLabel(identifierLabel, identifierValue).list();
        if (orphanServices != null) {
            for (var service : orphanServices.getItems()) {
                var namespace = service.getMetadata().getNamespace();
                kubeClient.services().inNamespace(namespace).delete(service);
            }
            log.info("Cleaned " + orphanServices.getItems().size() + " services");
        }
        var orphanPVCs = kubeClient.persistentVolumeClaims().inAnyNamespace().withLabel(identifierLabel, identifierValue).list();
        if (orphanPVCs != null) {
            for (var pvc : orphanPVCs.getItems()) {
                var namespace = pvc.getMetadata().getNamespace();
                kubeClient.persistentVolumeClaims().inNamespace(namespace).delete(pvc);
            }
            log.info("Cleaned " + orphanPVCs.getItems().size() + " PersistentVolumeClaims");
        }
    }

    public PodList getFailedAndUnknownPods() {
        var identifierLabel = environment.getProperty("proxy.identifier-label", "comp.polyu.edu.hk/vl-identifier");
        var identifierValue = environment.getProperty("proxy.identifier-value", "default-identifier");
        return kubeClient.pods().inAnyNamespace()
                .withLabel(identifierLabel, identifierValue)
                .withoutField("status.phase", "Pending")
                .withoutField("status.phase", "Running")
                .withoutField("status.phase", "Succeeded")
                .list();
    }

    private class ErrorPodsCleaner implements Runnable {
        @Override
        public void run() {
            log.info("Enable failed and unknown phase pods detection & cleaning");
            while (true) {
                var failedPods = getFailedAndUnknownPods();
                if (failedPods != null && !failedPods.getItems().isEmpty()) {
                    for (var pod : failedPods.getItems()) {
                        var proxyId = pod.getMetadata().getLabels().get("comp.polyu.edu.hk/vl-proxy-id");
                        proxyService.stopProxy(proxyService.getProxy(proxyId), true, true);
                        log.error("Cleaned error proxy {}", proxyId);
                    }
                }
                try {
                    Thread.sleep(30000);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }
}