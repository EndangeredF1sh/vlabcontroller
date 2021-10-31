package eu.openanalytics.containerproxy.model.spec;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProxySpec {
  @Getter @Setter private String id;
  @Getter @Setter private String displayName;
  @Getter @Setter private String description;
  @Getter @Setter private String logoURL;
  
  @Getter @Setter private ProxyAccessControl accessControl;
  @Getter @Setter private List<ContainerSpec> containerSpecs;
  @Getter @Setter private List<RuntimeSettingSpec> runtimeSettingSpecs;
  
  @Getter @Setter private Map<String, String> settings = new HashMap<>();
  
  /**
   * Returns the Kubernetes Pod Patch as JsonValue (i.e. array) for nice representation in API requests.
   */
  @Getter @Setter private String kubernetesPodPatches;
  @Getter @Setter private List<String> kubernetesAdditionalManifests = new ArrayList<>();
  @Getter @Setter private List<SubApplicationSpec> subApps;
  @Getter @Setter private String defaultTutorialLink;
  
  public void copy(ProxySpec target) {
    target.setId(id);
    target.setDisplayName(displayName);
    target.setDescription(description);
    target.setLogoURL(logoURL);
    target.setDefaultTutorialLink(defaultTutorialLink);
    
    if (accessControl != null) {
      if (target.getAccessControl() == null) target.setAccessControl(new ProxyAccessControl());
      accessControl.copy(target.getAccessControl());
    }
    
    if (containerSpecs != null) {
      if (target.getContainerSpecs() == null) target.setContainerSpecs(new ArrayList<>());
      for (ContainerSpec spec : containerSpecs) {
        ContainerSpec copy = new ContainerSpec();
        spec.copy(copy);
        target.getContainerSpecs().add(copy);
      }
    }
    
    if (runtimeSettingSpecs != null) {
      if (target.getRuntimeSettingSpecs() == null) target.setRuntimeSettingSpecs(new ArrayList<>());
      for (RuntimeSettingSpec spec : runtimeSettingSpecs) {
        RuntimeSettingSpec copy = new RuntimeSettingSpec();
        spec.copy(copy);
        target.getRuntimeSettingSpecs().add(copy);
      }
    }
    
    if (settings != null) {
      if (target.getSettings() == null) target.setSettings(new HashMap<>());
      target.getSettings().putAll(settings);
    }
    
    
    if (kubernetesPodPatches != null) {
      target.setKubernetesPodPatches(kubernetesPodPatches);
    }
    
    if (kubernetesAdditionalManifests != null) {
      target.setKubernetesAdditionalManifests(new ArrayList<>(kubernetesAdditionalManifests));
    }
    
    if (subApps != null) {
      target.setSubApps(subApps);
    }
  }
  
}
