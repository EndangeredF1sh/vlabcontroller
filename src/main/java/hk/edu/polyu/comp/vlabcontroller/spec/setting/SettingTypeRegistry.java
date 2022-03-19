package hk.edu.polyu.comp.vlabcontroller.spec.setting;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.model.spec.RuntimeSettingSpec;
import hk.edu.polyu.comp.vlabcontroller.spec.ProxySpecException;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry of known runtime setting types.
 * <p>
 * This class can apply runtime settings onto ProxySpecs.
 * <ol>
 * <li>First, it will try to resolve a setting type using the RuntimeSettingSpec of the setting.</li>
 * <li>If a matching type was found, an attempt will be made to apply the value of the setting onto the target ProxySpec,
 * using the rules of the setting type.</li>
 * </p>
 */
@Component
public class SettingTypeRegistry {
    @Setter(onMethod_ = {@Autowired(required = false)})
    private Map<String, IRuntimeSettingType> typeMap = new HashMap<>();

    public RuntimeSettingSpec resolveSpec(RuntimeSetting setting, ProxySpec proxySpec) {
        return proxySpec.getRuntimeSettingSpecs().stream().filter(s -> s.getName().equals(setting.getName())).findAny().orElse(null);
    }

    public IRuntimeSettingType resolveSpecType(RuntimeSettingSpec settingSpec) {
        var type = settingSpec.getType();
        if (type == null || type.isEmpty()) {
            //TODO try to determine the type via the spec config
            type = "setting.type.string";
        }
        return typeMap.get(type);
    }

    public void applySetting(RuntimeSetting setting, ProxySpec targetSpec) throws ProxySpecException {
        var settingSpec = resolveSpec(setting, targetSpec);
        if (settingSpec == null) return;

        var type = resolveSpecType(settingSpec);
        if (type == null) throw new ProxySpecException("Unknown setting type: " + settingSpec.getType());

        type.apply(setting, settingSpec, targetSpec);
    }
}
