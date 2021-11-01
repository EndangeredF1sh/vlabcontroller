package eu.openanalytics.containerproxy.spec;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.filebrowser")
public class FileBrowserProperties {
  @Getter @Setter private String containerImage;
  @Getter @Setter private String[] containerCmd;
  @Getter @Setter private Map<String, String> containerEnv;
  @Getter @Setter private String containerNetwork;
  @Getter @Setter private String[] containerVolumes;
  @Getter @Setter private String containerMemoryLimit;
  @Getter @Setter private String containerCpuLimit;
  @Getter @Setter private int port;
  
  @Getter @Setter private String kubernetesPodPatches;
  @Getter @Setter private List<String> kubernetesAdditionalManifests = new ArrayList<>();
  @Getter @Setter private Map<String, String> labels;
}
