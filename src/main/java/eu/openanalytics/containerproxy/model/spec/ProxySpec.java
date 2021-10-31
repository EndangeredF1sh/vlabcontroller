package eu.openanalytics.containerproxy.model.spec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProxySpec {
  
  private String id;
  private String displayName;
  private String description;
  private String logoURL;
  
  private ProxyAccessControl accessControl;
  private List<ContainerSpec> containerSpecs;
  private List<RuntimeSettingSpec> runtimeSettingSpecs;
  
  private Map<String, String> settings = new HashMap<>();
  
  private String kubernetesPodPatches;
  private List<String> kubernetesAdditionalManifests = new ArrayList<>();
  private List<SubApplicationSpec> subApps;
  private String defaultTutorialLink;
  
  public ProxySpec() {
    settings = new HashMap<>();
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getDisplayName() {
    return displayName;
  }
  
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getLogoURL() {
    return logoURL;
  }
  
  public void setLogoURL(String logoURL) {
    this.logoURL = logoURL;
  }
  
  public ProxyAccessControl getAccessControl() {
    return accessControl;
  }
  
  public void setAccessControl(ProxyAccessControl accessControl) {
    this.accessControl = accessControl;
  }
  
  public List<ContainerSpec> getContainerSpecs() {
    return containerSpecs;
  }
  
  public void setContainerSpecs(List<ContainerSpec> containerSpecs) {
    this.containerSpecs = containerSpecs;
  }
  
  public List<RuntimeSettingSpec> getRuntimeSettingSpecs() {
    return runtimeSettingSpecs;
  }
  
  public void setRuntimeSettingSpecs(List<RuntimeSettingSpec> runtimeSettingSpecs) {
    this.runtimeSettingSpecs = runtimeSettingSpecs;
  }
  
  public Map<String, String> getSettings() {
    return settings;
  }
  
  public void setSettings(Map<String, String> settings) {
    this.settings = settings;
  }
  
  /**
   * Returns the Kubernetes Pod Patch as JsonValue (i.e. array) for nice representation in API requests.
   */
  public String getKubernetesPodPatch() {
    return kubernetesPodPatches;
  }
  
  public void setKubernetesPodPatches(String kubernetesPodPatches) {
    this.kubernetesPodPatches = kubernetesPodPatches;
  }
  
  public List<String> getKubernetesAdditionalManifests() {
    return kubernetesAdditionalManifests;
  }
  
  public void setKubernetesAdditionalManifests(List<String> manifests) {
    this.kubernetesAdditionalManifests = manifests;
  }
  
  public List<SubApplicationSpec> getSubApps() {
    return subApps;
  }
  
  public void setSubApps(List<SubApplicationSpec> subApps) {
    this.subApps = subApps;
  }
  
  public String getDefaultTutorialLink() {
    return defaultTutorialLink;
  }
  
  public void setDefaultTutorialLink(String defaultTutorialLink) {
    this.defaultTutorialLink = defaultTutorialLink;
  }
  
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
      target.setKubernetesAdditionalManifests(kubernetesAdditionalManifests.stream().collect(Collectors.toList()));
    }
    
    if (subApps != null) {
      target.setSubApps(subApps);
    }
  }
  
}
