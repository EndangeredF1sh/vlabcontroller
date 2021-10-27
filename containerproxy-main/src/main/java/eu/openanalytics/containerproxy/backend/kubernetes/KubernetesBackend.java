package eu.openanalytics.containerproxy.backend.kubernetes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import eu.openanalytics.containerproxy.ContainerProxyException;
import eu.openanalytics.containerproxy.backend.AbstractContainerBackend;
import eu.openanalytics.containerproxy.model.runtime.Container;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.model.spec.ContainerSpec;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.spec.expression.SpecExpressionContext;
import eu.openanalytics.containerproxy.util.Retrying;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.util.Pair;

import javax.inject.Inject;
import javax.json.JsonPatch;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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
  private static final String PARAM_SERVICE = "service";
  private static final String PARAM_NAMESPACE = "namespace";
  
  private static final String SECRET_KEY_REF = "secretKeyRef";
  
  private final Logger log = LogManager.getLogger(KubernetesBackend.class);
  
  @Inject
  private PodPatcher podPatcher;
  
  @Inject
  private ProxyService proxyService;
  
  private KubernetesClient kubeClient;
  
  @Override
  public void initialize() throws ContainerProxyException {
    super.initialize();
    
    ConfigBuilder configBuilder = new ConfigBuilder();
    
    String masterUrl = getProperty(PROPERTY_URL);
    if (masterUrl != null) configBuilder.withMasterUrl(masterUrl);
    
    String certPath = getProperty(PROPERTY_CERT_PATH);
    if (certPath != null && Files.isDirectory(Paths.get(certPath))) {
      Path certFilePath = Paths.get(certPath, "ca.pem");
      if (Files.exists(certFilePath)) configBuilder.withCaCertFile(certFilePath.toString());
      certFilePath = Paths.get(certPath, "cert.pem");
      if (Files.exists(certFilePath)) configBuilder.withClientCertFile(certFilePath.toString());
      certFilePath = Paths.get(certPath, "key.pem");
      if (Files.exists(certFilePath)) configBuilder.withClientKeyFile(certFilePath.toString());
    }
    
    kubeClient = new DefaultKubernetesClient(configBuilder.build());
    
    cleanBeforeStart();
    
    Thread cleanFailedThread = new Thread(new ErrorPodsCleaner(), ErrorPodsCleaner.class.getSimpleName());
    cleanFailedThread.setDaemon(true);
    cleanFailedThread.start();
  }
  
  public void initialize(KubernetesClient client) {
    super.initialize();
    kubeClient = client;
    Thread cleanFailedThread = new Thread(new ErrorPodsCleaner(), ErrorPodsCleaner.class.getSimpleName());
    cleanFailedThread.setDaemon(true);
    cleanFailedThread.start();
  }
  
  @Override
  protected Container startContainer(ContainerSpec spec, Proxy proxy) throws Exception {
    Container container = new Container();
    container.setSpec(spec);
    container.setId(UUID.randomUUID().toString());
    
    String kubeNamespace = getProperty(PROPERTY_NAMESPACE, DEFAULT_NAMESPACE);
    String namespacePrefix = getProperty(PROPERTY_NAMESPACE_PREFIX);
    String identifierLabel = environment.getProperty("proxy.identifier-label", "openanalytics.eu/sp-identifier");
    String identifierValue = environment.getProperty("proxy.identifier-value", "default-identifier");
    boolean uidNamespace = Boolean.valueOf(getProperty(PROPERTY_UID_NAMESPACE, "false"));
    log.debug("UserID Namespace Mode:" + uidNamespace);
    if (uidNamespace) {
      kubeNamespace = Strings.isNullOrEmpty(namespacePrefix) ? proxy.getUserId() : namespacePrefix + "-" + proxy.getUserId();
    }
    String apiVersion = getProperty(PROPERTY_API_VERSION, DEFAULT_API_VERSION);
    
    String[] volumeStrings = Optional.ofNullable(spec.getVolumes()).orElse(new String[]{});
    List<Volume> volumes = new ArrayList<>();
    VolumeMount[] volumeMounts = new VolumeMount[volumeStrings.length];
    for (int i = 0; i < volumeStrings.length; i++) {
      String[] volume = volumeStrings[i].split(":");
      String hostSource = volume[0];
      String containerDest = volume[1];
      String name = "shinyproxy-volume-" + i;
      volumes.add(new VolumeBuilder()
        .withNewHostPath(hostSource, "")
        .withName(name)
        .build());
      volumeMounts[i] = new VolumeMountBuilder()
        .withMountPath(containerDest)
        .withName(name)
        .build();
    }
    
    List<EnvVar> envVars = new ArrayList<>();
    for (String envString : buildEnv(spec, proxy)) {
      String[] e = envString.split("=");
      if (e.length == 1) e = new String[]{e[0], ""};
      if (e.length > 2) e[1] = envString.substring(envString.indexOf('=') + 1);
      
      if (e[1].toLowerCase().startsWith(SECRET_KEY_REF.toLowerCase())) {
        String[] ref = e[1].split(":");
        if (ref.length != 3) {
          log.warn(String.format("Invalid secret key reference: %s. Expected format: '%s:<name>:<key>'", envString, SECRET_KEY_REF));
          continue;
        }
        envVars.add(new EnvVar(e[0], null, new EnvVarSourceBuilder()
          .withSecretKeyRef(new SecretKeySelectorBuilder()
            .withName(ref[1])
            .withKey(ref[2])
            .build())
          .build()));
      } else {
        envVars.add(new EnvVar(e[0], e[1], null));
      }
    }
    
    SecurityContext security = new SecurityContextBuilder()
      .withPrivileged(isPrivileged() || spec.isPrivileged())
      .build();
    
    ResourceRequirementsBuilder resourceRequirementsBuilder = new ResourceRequirementsBuilder();
    resourceRequirementsBuilder.addToRequests("cpu", Optional.ofNullable(spec.getCpuRequest()).map(s -> new Quantity(s)).orElse(null));
    resourceRequirementsBuilder.addToLimits("cpu", Optional.ofNullable(spec.getCpuLimit()).map(s -> new Quantity(s)).orElse(null));
    resourceRequirementsBuilder.addToRequests("memory", Optional.ofNullable(spec.getMemoryRequest()).map(s -> new Quantity(s)).orElse(null));
    resourceRequirementsBuilder.addToLimits("memory", Optional.ofNullable(spec.getMemoryLimit()).map(s -> new Quantity(s)).orElse(null));
    
    List<ContainerPort> containerPorts = spec.getPortMapping().values().stream()
      .map(p -> new ContainerPortBuilder().withContainerPort(p).build())
      .collect(Collectors.toList());
    
    ContainerBuilder containerBuilder = new ContainerBuilder()
      .withImage(spec.getImage())
      .withCommand(spec.getCmd())
      .withName("sp-container-" + container.getId())
      .withPorts(containerPorts)
      .withVolumeMounts(volumeMounts)
      .withSecurityContext(security)
      .withResources(resourceRequirementsBuilder.build())
      .withEnv(envVars);
    
    String imagePullPolicy = getProperty(PROPERTY_IMG_PULL_POLICY);
    if (imagePullPolicy != null) containerBuilder.withImagePullPolicy(imagePullPolicy);
    
    String[] imagePullSecrets = {};
    String imagePullSecret = getProperty(PROPERTY_IMG_PULL_SECRET);
    if (imagePullSecret == null) {
      String imagePullSecretArray = getProperty(PROPERTY_IMG_PULL_SECRETS);
      if (imagePullSecretArray != null) imagePullSecrets = imagePullSecretArray.split(",");
    } else {
      imagePullSecrets = new String[]{imagePullSecret};
    }
    
    ObjectMetaBuilder objectMetaBuilder = new ObjectMetaBuilder()
      .withNamespace(kubeNamespace)
      .withName("sp-pod-" + container.getId())
      .addToLabels(spec.getLabels())
      .addToLabels(identifierLabel, identifierValue)
      .addToLabels("app", container.getId());
    
    for (Map.Entry<String, Pair<Boolean, String>> runtimeLabel : spec.getRuntimeLabels().entrySet()) {
      if (runtimeLabel.getValue().getFirst()) {
        objectMetaBuilder.addToLabels(runtimeLabel.getKey(), runtimeLabel.getValue().getSecond());
      } else {
        objectMetaBuilder.addToAnnotations(runtimeLabel.getKey(), runtimeLabel.getValue().getSecond());
      }
    }
    
    PodBuilder podBuilder = new PodBuilder()
      .withApiVersion(apiVersion)
      .withKind("Pod")
      .withMetadata(objectMetaBuilder.build());
    
    PodSpec podSpec = new PodSpec();
    podSpec.setContainers(Collections.singletonList(containerBuilder.build()));
    podSpec.setVolumes(volumes);
    podSpec.setImagePullSecrets(Arrays.stream(imagePullSecrets)
      .map(LocalObjectReference::new).collect(Collectors.toList()));
    
    String nodeSelectorString = getProperty(PROPERTY_NODE_SELECTOR);
    if (nodeSelectorString != null) {
      podSpec.setNodeSelector(Splitter.on(",").withKeyValueSeparator("=").split(nodeSelectorString));
    }
    
    JsonPatch patch = readPatchFromSpec(spec, proxy);
    
    Pod startupPod = podBuilder.withSpec(podSpec).build();
    Pod patchedPod = podPatcher.patchWithDebug(startupPod, patch);
    final String effectiveKubeNamespace = patchedPod.getMetadata().getNamespace(); // use the namespace of the patched Pod, in case the patch changes the namespace.
    container.getParameters().put(PARAM_NAMESPACE, effectiveKubeNamespace);
    
    // create additional manifests -> use the effective (i.e. patched) namespace if no namespace is provided
    createAdditionalManifests(proxy, effectiveKubeNamespace);
    
    Pod startedPod = kubeClient.pods().inNamespace(effectiveKubeNamespace).create(patchedPod);
    
    int totalWaitMs = Integer.parseInt(environment.getProperty("proxy.kubernetes.pod-wait-time", "60000"));
    int maxTries = totalWaitMs / 1000;
    Retrying.retry(i -> {
        if (!Readiness.isPodReady(kubeClient.resource(startedPod).fromServer().get())) {
          if (i > 1 && log != null)
            log.debug(String.format("Container not ready yet, trying again (%d/%d)", i, maxTries));
          return false;
        }
        return true;
      }
      , maxTries, 1000);
    if (!Readiness.isPodReady(kubeClient.resource(startedPod).fromServer().get())) {
      Pod pod = kubeClient.resource(startedPod).fromServer().get();
      container.getParameters().put(PARAM_POD, pod);
      proxy.getContainers().add(container);
      throw new ContainerProxyException("Container did not become ready in time");
    }
    Pod pod = kubeClient.resource(startedPod).fromServer().get();
    
    Service service = null;
    if (isUseInternalNetwork()) {
      // If SP runs inside the cluster, it can access pods directly and doesn't need any port publishing service.
    } else {
      List<ServicePort> servicePorts = spec.getPortMapping().values().stream()
        .map(p -> new ServicePortBuilder().withPort(p).build())
        .collect(Collectors.toList());
      
      Service startupService = new ServiceBuilder()
        .withApiVersion(apiVersion)
        .withKind("Service")
        .withNewMetadata()
        .withName("sp-service-" + container.getId())
        .addToLabels(RUNTIME_LABEL_PROXY_ID, proxy.getId())
        .addToLabels(RUNTIME_LABEL_PROXIED_APP, "true")
        .addToLabels(RUNTIME_LABEL_INSTANCE, instanceId)
        .addToLabels(identifierLabel, identifierValue)
        .addToLabels(spec.getLabels())
        .endMetadata()
        .withNewSpec()
        .addToSelector("app", container.getId())
        .withType("NodePort")
        .withPorts(servicePorts)
        .endSpec()
        .build();
      kubeClient.services().inNamespace(effectiveKubeNamespace).createOrReplace(startupService);
      // Workaround: waitUntilReady appears to be buggy.
      Retrying.retry(i -> isServiceReady(kubeClient.resource(startupService).fromServer().get()), 60, 1000);
      
      service = kubeClient.resource(startupService).fromServer().get();
    }
    
    container.getParameters().put(PARAM_POD, pod);
    container.getParameters().put(PARAM_SERVICE, service);
    
    // Calculate proxy routes for all configured ports.
    for (String mappingKey : spec.getPortMapping().keySet()) {
      int containerPort = spec.getPortMapping().get(mappingKey);
      
      int servicePort = -1;
      if (service != null) servicePort = service.getSpec().getPorts().stream()
        .filter(p -> p.getPort() == containerPort).map(p -> p.getNodePort())
        .findAny().orElse(-1);
      
      String mapping = mappingStrategy.createMapping(mappingKey, container, proxy);
      URI target = calculateTarget(container, containerPort, servicePort);
      proxy.getTargets().put(mapping, target);
    }
    
    
    return container;
  }
  
  private JsonPatch readPatchFromSpec(ContainerSpec containerSpec, Proxy proxy) throws JsonProcessingException {
    String patchAsString = proxy.getSpec().getKubernetesPodPatch();
    if (patchAsString == null) {
      return null;
    }
    
    // resolve expressions
    SpecExpressionContext context = SpecExpressionContext.create(containerSpec, proxy, proxy.getSpec());
    String expressionAwarePatch = expressionResolver.evaluateToString(patchAsString, context);
    
    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
    yamlReader.registerModule(new JSR353Module());
    return yamlReader.readValue(expressionAwarePatch, JsonPatch.class);
  }
  
  /**
   * Creates the extra manifests/resources defined in the ProxySpec.
   * <p>
   * The resource will only be created if it does not already exist.
   */
  private void createAdditionalManifests(Proxy proxy, String namespace) {
    for (HasMetadata fullObject : getAdditionManifestsAsObjects(proxy, namespace)) {
      if (kubeClient.resource(fullObject).fromServer().get() == null) {
        String identifierLabel = environment.getProperty("proxy.identifier-label", "openanalytics.eu/sp-identifier");
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
  
  /**
   * Converts the additional manifests of the spec into HasMetadata objects.
   * When the resource has no namespace definition, the provided namespace
   * parameter will be used.
   */
  private List<HasMetadata> getAdditionManifestsAsObjects(Proxy proxy, String namespace) {
    SpecExpressionContext context = SpecExpressionContext.create(proxy, proxy.getSpec());
    
    ArrayList<HasMetadata> result = new ArrayList<HasMetadata>();
    for (String manifest : proxy.getSpec().getKubernetesAdditionalManifests()) {
      String expressionManifest = expressionResolver.evaluateToString(manifest, context);
      HasMetadata object = Serialization.unmarshal(new ByteArrayInputStream(expressionManifest.getBytes())); // used to determine whether the manifest has specified a namespace
      
      HasMetadata fullObject = kubeClient.load(new ByteArrayInputStream(expressionManifest.getBytes())).get().get(0);
      if (object.getMetadata().getNamespace() == null) {
        // the load method (in some cases) automatically sets a namepsace when no namespace is provided
        // therefore we overwrite this namespace with the namsepace of the pod.
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
  
  protected URI calculateTarget(Container container, int containerPort, int servicePort) throws Exception {
    String targetProtocol = getProperty(PROPERTY_CONTAINER_PROTOCOL, DEFAULT_TARGET_PROTOCOL);
    String targetHostName;
    int targetPort;
    
    Pod pod = Pod.class.cast(container.getParameters().get(PARAM_POD));
    
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
  protected void doStopProxy(Proxy proxy) throws Exception {
    // wait a bit to make sure there will be a pod to remove
    // prevent orphan pods issue (start an app and sign out quickly)
    int retry = 20;
    int interval = 3000;
    boolean responded = false;
    for (int i = 0; i < retry; i++) {
      if (proxy.getContainers().isEmpty()) {
        log.debug("Proxy pod unresponsive, retrying ({}/{})", i + 1, retry);
        Thread.sleep(interval);
      } else {
        responded = true;
        log.debug("Pod responded, removing...");
        break;
      }
    }
    if (!responded) {
      String kubeNamespace = getProperty(PROPERTY_NAMESPACE, DEFAULT_NAMESPACE);
      String namespacePrefix = getProperty(PROPERTY_NAMESPACE_PREFIX);
      if (Boolean.parseBoolean(getProperty(PROPERTY_UID_NAMESPACE, "false"))) {
        kubeNamespace = Strings.isNullOrEmpty(namespacePrefix) ? proxy.getUserId() : namespacePrefix + "-" + proxy.getUserId();
      }
      // we have to delete additional manifests and services even though we can't find pod
      for (HasMetadata fullObject : getAdditionManifestsAsObjects(proxy, kubeNamespace)) {
        kubeClient.resource(fullObject).delete();
      }
      kubeClient.services().inNamespace(kubeNamespace).withLabel(RUNTIME_LABEL_PROXY_ID, proxy.getId()).delete();
      throw new ContainerProxyException("Failed to stop container: no pod was founded");
    }
    for (Container container : proxy.getContainers()) {
      String kubeNamespace = container.getParameters().get(PARAM_NAMESPACE).toString();
      if (kubeNamespace == null) {
        kubeNamespace = getProperty(PROPERTY_NAMESPACE, DEFAULT_NAMESPACE);
      }
      
      Pod pod = Pod.class.cast(container.getParameters().get(PARAM_POD));
      if (pod != null) kubeClient.pods().inNamespace(kubeNamespace).delete(pod);
      Service service = Service.class.cast(container.getParameters().get(PARAM_SERVICE));
      if (service != null) kubeClient.services().inNamespace(kubeNamespace).delete(service);
      
      // delete additional manifests
      for (HasMetadata fullObject : getAdditionManifestsAsObjects(proxy, kubeNamespace)) {
        kubeClient.resource(fullObject).delete();
      }
    }
  }
  
  @Override
  public BiConsumer<OutputStream, OutputStream> getOutputAttacher(Proxy proxy) {
    if (proxy.getContainers().isEmpty()) return null;
    return (stdOut, stdErr) -> {
      try {
        Container container = proxy.getContainers().get(0);
        String kubeNamespace = container.getParameters().get(PARAM_NAMESPACE).toString();
        if (kubeNamespace == null) {
          kubeNamespace = getProperty(PROPERTY_NAMESPACE, DEFAULT_NAMESPACE);
        }
        LogWatch watcher = kubeClient.pods().inNamespace(kubeNamespace).withName("sp-pod-" + container.getId()).watchLog();
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
    String identifierLabel = environment.getProperty("proxy.identifier-label", "openanalytics.eu/sp-identifier");
    String identifierValue = environment.getProperty("proxy.identifier-value", "default-identifier");
    PodList orphanPods = kubeClient.pods().inAnyNamespace().withLabel(identifierLabel, identifierValue).list();
    if (orphanPods != null) {
      for (Pod pod : orphanPods.getItems()) {
        String namespace = pod.getMetadata().getNamespace();
        kubeClient.pods().inNamespace(namespace).delete(pod);
      }
      log.info("Cleaned " + orphanPods.getItems().size() + " pods");
    }
    ServiceList orphanServices = kubeClient.services().inAnyNamespace().withLabel(identifierLabel, identifierValue).list();
    if (orphanServices != null) {
      for (Service service : orphanServices.getItems()) {
        String namespace = service.getMetadata().getNamespace();
        kubeClient.services().inNamespace(namespace).delete(service);
      }
      log.info("Cleaned " + orphanServices.getItems().size() + " services");
    }
    PersistentVolumeClaimList orphanPVCs = kubeClient.persistentVolumeClaims().inAnyNamespace().withLabel(identifierLabel, identifierValue).list();
    if (orphanPVCs != null) {
      for (PersistentVolumeClaim pvc : orphanPVCs.getItems()) {
        String namespace = pvc.getMetadata().getNamespace();
        kubeClient.persistentVolumeClaims().inNamespace(namespace).delete(pvc);
      }
      log.info("Cleaned " + orphanPVCs.getItems().size() + " PersistentVolumeClaims");
    }
  }
  
  public PodList getFailedAndUnknownPods() {
    String identifierLabel = environment.getProperty("proxy.identifier-label", "openanalytics.eu/sp-identifier");
    String identifierValue = environment.getProperty("proxy.identifier-value", "default-identifier");
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
        PodList failedPods = getFailedAndUnknownPods();
        if (failedPods != null && !failedPods.getItems().isEmpty()) {
          for (Pod pod : failedPods.getItems()) {
            String proxyId = pod.getMetadata().getLabels().get("openanalytics.eu/sp-proxy-id");
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
