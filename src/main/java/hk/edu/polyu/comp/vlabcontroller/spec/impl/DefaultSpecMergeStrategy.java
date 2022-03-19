package hk.edu.polyu.comp.vlabcontroller.spec.impl;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.spec.IProxySpecMergeStrategy;
import hk.edu.polyu.comp.vlabcontroller.spec.ProxySpecException;
import hk.edu.polyu.comp.vlabcontroller.spec.setting.SettingTypeRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * This default merge strategy allows any combination of base spec, runtime spec and runtime settings.
 */
@Component
public class DefaultSpecMergeStrategy implements IProxySpecMergeStrategy {
    private final SettingTypeRegistry settingTypeRegistry;

    public DefaultSpecMergeStrategy(SettingTypeRegistry settingTypeRegistry) {
        this.settingTypeRegistry = settingTypeRegistry;
    }

    @Override
    public ProxySpec merge(ProxySpec baseSpec, ProxySpec runtimeSpec, Set<RuntimeSetting> runtimeSettings) throws ProxySpecException {
        if (baseSpec == null && runtimeSpec == null)
            throw new ProxySpecException("No base or runtime proxy spec provided");

        ProxySpec finalSpec = new ProxySpec();
        copySpec(baseSpec, finalSpec);
        copySpec(runtimeSpec, finalSpec);

        if (runtimeSettings != null) {
            for (RuntimeSetting setting : runtimeSettings) {
                settingTypeRegistry.applySetting(setting, finalSpec);
            }
        }

        if (finalSpec.getId() == null) {
            var id = UUID.randomUUID().toString();
            finalSpec.setId(id);
            for (var containerSpec : finalSpec.getContainerSpecs()) {
                containerSpec.getEnv().put("PUBLIC_PATH", DefaultSpecProvider.getPublicPath(id));
            }
        }
        return finalSpec;
    }

    protected void copySpec(ProxySpec from, ProxySpec to) {
        if (from == null || to == null) return;
        from.copy(to);
    }
}
