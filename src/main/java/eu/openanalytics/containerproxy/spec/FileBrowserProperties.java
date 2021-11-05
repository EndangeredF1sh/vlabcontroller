package eu.openanalytics.containerproxy.spec;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.filebrowser")
public class FileBrowserProperties {
    @Getter
    @Setter
    private String image;
    @Getter
    @Setter
    private List<String> cmd = List.of();
    @Getter
    @Setter
    private Map<String, String> env = new HashMap<>();
    @Getter
    @Setter
    private String network;
    @Getter
    @Setter
    private List<String> volumes = new ArrayList<>();
    @Getter
    @Setter
    private String memoryLimit;
    @Getter
    @Setter
    private String cpuLimit;
    @Getter
    @Setter
    private int port;

    @Getter
    @Setter
    private String kubernetesPodPatches;
    @Getter
    @Setter
    private List<String> kubernetesAdditionalManifests = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, String> labels;
}
