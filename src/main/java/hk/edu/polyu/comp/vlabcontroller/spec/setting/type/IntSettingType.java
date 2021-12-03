package hk.edu.polyu.comp.vlabcontroller.spec.setting.type;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.RuntimeSettingSpec;
import hk.edu.polyu.comp.vlabcontroller.spec.ProxySpecException;
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
