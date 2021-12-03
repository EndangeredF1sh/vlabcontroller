package hk.edu.polyu.comp.vlabcontroller.spec.setting.type;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.RuntimeSettingSpec;
import org.springframework.stereotype.Component;

@Component("setting.type.string")
public class StringSettingType extends AbstractSettingType {

    @Override
    protected Object getValue(RuntimeSetting setting, RuntimeSettingSpec settingSpec) {
        return String.valueOf(setting.getValue());
    }

}
