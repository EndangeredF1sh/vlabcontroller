package hk.edu.polyu.comp.vlabcontroller.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyKubernetesProperties {
    Duration podWaitTime = Duration.ofMinutes(1);
    boolean debugPatches = false;
    boolean internalNetworking = false;
    boolean privileged = false;
    String url;
    String certPath;
    String containerProtocol = "http";
    String namespace = "default";
    String apiVersion = "v1";
    String imagePullPolicy;
    List<String> imagePullSecrets = new ArrayList<>();
    String imagePullSecret;
    String nodeSelector;
    boolean customNamespace = false;
    String namespacePrefix;
    String secureRuntimeName = "kata-qemu";
}
