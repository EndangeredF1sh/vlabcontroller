package hk.edu.polyu.comp.vlabcontroller.spec.impl;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.spec.IProxySpecMergeStrategy;
import hk.edu.polyu.comp.vlabcontroller.spec.ProxySpecException;
import hk.edu.polyu.comp.vlabcontroller.spec.setting.SettingTypeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * This default merge strategy allows any combination of base spec, runtime spec and runtime settings.
 */
@Component
@RequiredArgsConstructor
public class DefaultSpecMergeStrategy implements IProxySpecMergeStrategy {
    private final SettingTypeRegistry settingTypeRegistry;

    @Override
    public ProxySpec merge(ProxySpec baseSpec, ProxySpec runtimeSpec, Set<RuntimeSetting> runtimeSettings) throws ProxySpecException {
        val hasBase = baseSpec != null;
        val hasRuntime = runtimeSpec != null;
        if (!(hasBase || hasRuntime))
            throw new ProxySpecException("No base or runtime proxy spec provided");

        var finalSpec = (hasBase && hasRuntime)
            ? runtimeSpec.copyToBuilder(baseSpec.copyBuilder()).build()
            : (hasBase ? baseSpec : runtimeSpec);

        for (var setting : Optional.ofNullable(runtimeSettings).orElse(Set.of())) {
            settingTypeRegistry.applySetting(setting, finalSpec);
        }

        if (finalSpec.getId() == null) {
            finalSpec.setId(UUID.randomUUID().toString());
            finalSpec.populateContainerSpecPublicPathById();
        }
        return finalSpec;
    }
}
