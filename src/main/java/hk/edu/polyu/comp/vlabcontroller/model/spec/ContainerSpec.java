package hk.edu.polyu.comp.vlabcontroller.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import io.fabric8.kubernetes.api.model.VolumeMount;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContainerSpec {
    private String image;
    private String name;
    @Singular("cmd") private List<String> cmd = new ArrayList<>();
    @Singular("env") private Map<String, String> env = new HashMap<>();
    private String envFile;
    private String network;
    @Singular private List<String> networkConnections = new ArrayList<>();
    @Singular("dns") private List<String> dns = new ArrayList<>();
    @Singular private List<EntryPointSpec> entryPoints = new ArrayList<>();
    @Singular("portMapping") private Map<String, Integer> portMapping = new HashMap<>();
    private boolean privileged;
    @Builder.Default private ResourceSpec resources = new ResourceSpec();
    @Deprecated @Singular("DEPRECATED_volumeMount") private List<VolumeMount> volumeMount = new ArrayList<>();
    @Singular private List<VolumeMount> volumeMounts = new ArrayList<>();
    @Singular private List<VolumeMount> adminVolumeMounts = new ArrayList<>();
    @Singular private Map<String, String> settings = new HashMap<>();

    /**
     * RuntimeLabels are labels which are calculated at runtime and contain metadata about the proxy.
     * These should not be included in API responses.
     * <p>
     * The boolean in the pair indicates whether the value is "safe". Safe values are calculated by
     * VLabController itself and contain no user data.
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

    @Deprecated(since = "1.0.2", forRemoval = true)
    public void setVolumeMount(List<VolumeMount> volumeMount) {
        log.warn("containerSpec[].volumeMount is deprecated in 1.0.2+, unavailable in 1.1+, use containerSpec[].volumeMounts instead");
        setVolumeMounts(volumeMount);
        this.volumeMount = volumeMounts;
    }

    @Deprecated(since = "1.0.2", forRemoval = true)
    public List<VolumeMount> getVolumeMount() {
        return volumeMount;
    }

    public void populatePublicPathById(String id) {
        var map = getEnv().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        map.put("PUBLIC_PATH", ProxyProperties.getPublicPath(id));
        setEnv(Collections.unmodifiableMap(map));
    }

    public ContainerSpec copy() {
        return this.toBuilder()
            .portMapping(entryPoints.stream().collect(Collectors.toMap(x -> String.format("port_mappings/%d", x.getPort()), EntryPointSpec::getPort)))
            .build();
    }
}
