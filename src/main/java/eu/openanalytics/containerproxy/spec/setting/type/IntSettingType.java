package eu.openanalytics.containerproxy.spec.setting.type;

import eu.openanalytics.containerproxy.model.runtime.RuntimeSetting;
import eu.openanalytics.containerproxy.model.spec.RuntimeSettingSpec;
import eu.openanalytics.containerproxy.spec.ProxySpecException;
import org.springframework.stereotype.Component;

@Component("setting.type.int")
public class IntSettingType extends AbstractSettingType {

    @Override
    protected Object getValue(RuntimeSetting setting, RuntimeSettingSpec settingSpec) {
        if (setting.getValue() == null) return null;
        else if (setting.getValue() instanceof Number) return ((Number) setting.getValue()).intValue();
        else
            throw new ProxySpecException("Setting value is not an integer: " + setting.getName() + ": " + setting.getValue());
    }

}
