package eu.openanalytics.containerproxy.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class ContainerSpec {
  
  @Getter @Setter private String image;
  @Getter @Setter private String[] cmd;
  @Getter @Setter private Map<String, String> env = new HashMap<>();
  @Getter @Setter private String envFile;
  @Getter @Setter private String network;
  @Getter @Setter private String[] networkConnections;
  @Getter @Setter private String[] dns;
  @Getter @Setter private String[] volumes;
  @Getter @Setter private List<Integer> ports = new ArrayList<>();
  @Getter @Setter private Map<String, Integer> portMapping = new HashMap<>();
  @Getter @Setter private boolean privileged;
  @Getter @Setter private String memoryRequest;
  @Getter @Setter private String memoryLimit;
  @Getter @Setter private String cpuRequest;
  @Getter @Setter private String cpuLimit;
  @Getter @Setter private Map<String, String> labels = new HashMap<>();
  @Getter @Setter private Map<String, String> settings = new HashMap<>();
  
  /**
   * RuntimeLabels are labels which are calculated at runtime and contain metadata about the proxy.
   * These should not be included in API responses.
   * <p>
   * The boolean in the pair indicates whether the value is "safe". Safe values are calculated by
   * ShinyProxy itself and contain no user data.
   * In practice, safe labels are saved as Kubernetes labels and non-safe labels are saved as
   * Kubernetes annotations.
   */
  @Setter private Map<String, Pair<Boolean, String>> runtimeLabels = new HashMap<>();
  
  @JsonIgnore
  public Map<String, Pair<Boolean, String>> getRuntimeLabels() {
    return runtimeLabels;
  }
  
  public void addRuntimeLabel(String key, Boolean safe, String value) {
    if (this.runtimeLabels.containsKey(key)) {
      throw new IllegalStateException("Cannot add duplicate label with key " + key);
    } else {
      runtimeLabels.put(key, Pair.of(safe, value));
    }
  }
  
  public void copy(ContainerSpec target) {
    target.setImage(image);
    if (cmd != null) target.setCmd(Arrays.copyOf(cmd, cmd.length));
    if (env != null) {
      if (target.getEnv() == null) target.setEnv(new HashMap<>());
      target.getEnv().putAll(env);
    }
    target.setEnvFile(envFile);
    target.setNetwork(network);
    if (networkConnections != null)
      target.setNetworkConnections(Arrays.copyOf(networkConnections, networkConnections.length));
    if (dns != null) target.setDns(Arrays.copyOf(dns, dns.length));
    if (volumes != null) target.setVolumes(Arrays.copyOf(volumes, volumes.length));
    if (portMapping != null) {
      if (target.getPortMapping() == null) target.setPortMapping(new HashMap<>());
      target.getPortMapping().putAll(portMapping);
    }
    if (ports != null) {
      if (target.getPorts() == null) target.setPorts(new ArrayList<>());
      target.getPorts().addAll(this.getPorts());
      target.getPortMapping().putAll(
        ports.stream().collect(Collectors.toMap(x -> String.format("port_mappings/%d", x), x -> x))
      );
    }
    
    target.setMemoryRequest(memoryRequest);
    target.setMemoryLimit(memoryLimit);
    target.setCpuRequest(cpuRequest);
    target.setCpuLimit(cpuLimit);
    target.setPrivileged(privileged);
    if (labels != null) {
      if (target.getLabels() == null) target.setLabels(new HashMap<>());
      target.getLabels().putAll(labels);
    }
    if (settings != null) {
      if (target.getSettings() == null) target.setSettings(new HashMap<>());
      target.getSettings().putAll(settings);
    }
  }
}
