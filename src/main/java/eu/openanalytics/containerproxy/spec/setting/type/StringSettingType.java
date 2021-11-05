package eu.openanalytics.containerproxy.spec.setting.type;

import eu.openanalytics.containerproxy.model.runtime.RuntimeSetting;
import eu.openanalytics.containerproxy.model.spec.RuntimeSettingSpec;
import org.springframework.stereotype.Component;

@Component("setting.type.string")
public class StringSettingType extends AbstractSettingType {

    @Override
    protected Object getValue(RuntimeSetting setting, RuntimeSettingSpec settingSpec) {
        return String.valueOf(setting.getValue());
    }

}
