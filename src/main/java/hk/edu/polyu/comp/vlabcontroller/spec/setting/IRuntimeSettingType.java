package hk.edu.polyu.comp.vlabcontroller.spec.setting;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.model.spec.RuntimeSettingSpec;
import hk.edu.polyu.comp.vlabcontroller.spec.ProxySpecException;

public interface IRuntimeSettingType {

    void apply(RuntimeSetting setting, RuntimeSettingSpec settingSpec, ProxySpec targetSpec) throws ProxySpecException;

}
