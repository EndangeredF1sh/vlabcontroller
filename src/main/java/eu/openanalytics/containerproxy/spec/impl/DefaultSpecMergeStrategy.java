package eu.openanalytics.containerproxy.spec.impl;

import eu.openanalytics.containerproxy.model.runtime.RuntimeSetting;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.spec.IProxySpecMergeStrategy;
import eu.openanalytics.containerproxy.spec.ProxySpecException;
import eu.openanalytics.containerproxy.spec.setting.SettingTypeRegistry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Set;
import java.util.UUID;

/**
 * This default merge strategy allows any combination of base spec, runtime spec and runtime settings.
 */
@Component
public class DefaultSpecMergeStrategy implements IProxySpecMergeStrategy {
  
  @Inject
  private SettingTypeRegistry settingTypeRegistry;
  
  @Override
  public ProxySpec merge(ProxySpec baseSpec, ProxySpec runtimeSpec, Set<RuntimeSetting> runtimeSettings) throws ProxySpecException {
    if (baseSpec == null && runtimeSpec == null) throw new ProxySpecException("No base or runtime proxy spec provided");
    
    ProxySpec finalSpec = new ProxySpec();
    copySpec(baseSpec, finalSpec);
    copySpec(runtimeSpec, finalSpec);
    
    if (runtimeSettings != null) {
      for (RuntimeSetting setting : runtimeSettings) {
        settingTypeRegistry.applySetting(setting, finalSpec);
      }
    }
    
    if (finalSpec.getId() == null) finalSpec.setId(UUID.randomUUID().toString());
    return finalSpec;
  }
  
  protected void copySpec(ProxySpec from, ProxySpec to) {
    if (from == null || to == null) return;
    from.copy(to);
  }
}
