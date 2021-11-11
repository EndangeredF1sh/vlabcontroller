package eu.openanalytics.containerproxy.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class ContainerSpec {
    @Getter
    @Setter
    private String image;
    @Getter
    @Setter
    private List<String> cmd = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, String> env = new HashMap<>();
    @Getter
    @Setter
    private String envFile;
    @Getter
    @Setter
    private String network;
    @Getter
    @Setter
    private List<String> networkConnections = new ArrayList<>();
    @Getter
    @Setter
    private List<String> dns = new ArrayList<>();
    @Getter
    @Setter
    private List<String> volumes = new ArrayList<>();
    @Getter
    @Setter
    private List<Integer> ports = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, Integer> portMapping = new HashMap<>();
    @Getter
    @Setter
    private boolean privileged;
    @Getter
    @Setter
    private String memoryRequest;
    @Getter
    @Setter
    private String memoryLimit;
    @Getter
    @Setter
    private String cpuRequest;
    @Getter
    @Setter
    private String cpuLimit;
    @Getter
    @Setter
    private Map<String, String> settings = new HashMap<>();

    /**
     * RuntimeLabels are labels which are calculated at runtime and contain metadata about the proxy.
     * These should not be included in API responses.
     * <p>
     * The boolean in the pair indicates whether the value is "safe". Safe values are calculated by
     * ShinyProxy itself and contain no user data.
     * In practice, safe labels are saved as Kubernetes labels and non-safe labels are saved as
     * Kubernetes annotations.
     */
    @Setter
    private Map<String, Pair<Boolean, String>> runtimeLabels = new HashMap<>();

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
        target.getCmd().addAll(cmd);
        target.getEnv().putAll(env);
        target.setEnvFile(envFile);
        target.setNetwork(network);
        target.getNetworkConnections().addAll(networkConnections);
        target.getDns().addAll(dns);
        target.getVolumes().addAll(volumes);
        target.getPorts().addAll(ports);
        target.getPortMapping().putAll(portMapping);
        target.getPortMapping().putAll(ports.stream().collect(Collectors.toMap(x -> String.format("port_mappings/%d", x), x -> x)));
        target.setMemoryRequest(memoryRequest);
        target.setMemoryLimit(memoryLimit);
        target.setCpuRequest(cpuRequest);
        target.setCpuLimit(cpuLimit);
        target.setPrivileged(privileged);
        target.getSettings().putAll(settings);
    }
}
