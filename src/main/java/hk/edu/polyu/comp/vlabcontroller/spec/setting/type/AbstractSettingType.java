package hk.edu.polyu.comp.vlabcontroller.spec.setting.type;

import javax.inject.Inject;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.model.spec.RuntimeSettingSpec;
import hk.edu.polyu.comp.vlabcontroller.spec.ProxySpecException;
import hk.edu.polyu.comp.vlabcontroller.spec.setting.IRuntimeSettingType;
import hk.edu.polyu.comp.vlabcontroller.spec.setting.SettingSpecMapper;
import lombok.Setter;

/**
 * Example runtime settings:
 * <p>
 * proxy:
 * specs:
 * - name: 01_hello
 * runtime-setting-specs:
 * - name: container.cpu
 * enum: [2,4,8]
 * - name: container.memory
 * range: [2,12]
 * <p>
 * Supported types: String, StringPattern, StringEnum, Int, IntRange, IntEnum, Float, FloatRange, FloatEnum, ...
 * <p>
 * - If type is omitted, can be derived from config? i.e. range -> IntRange
 * - If mapping is omitted, can be derived from name? i.e. container.cpu -> containerSpec.setCpu(int)
 * <p>
 * Custom types: name doesn't map to a spec field. Needs a custom class to resolve.
 * E.g.
 * - name: container.class
 * type: MyContainerClass
 * MyContainerClass offers 3 classes: low, med, hi
 * Each class translates into several settings, e.g. cpu & memory
 */
public abstract class AbstractSettingType implements IRuntimeSettingType {
    @Setter(onMethod_ = {@Inject})
    protected SettingSpecMapper mapper;

    @Override
    public void apply(RuntimeSetting setting, RuntimeSettingSpec settingSpec, ProxySpec targetSpec) throws ProxySpecException {
        var value = getValue(setting, settingSpec);
        if (value == null) return;
        mapper.mapValue(value, settingSpec, targetSpec);
    }

    protected abstract Object getValue(RuntimeSetting setting, RuntimeSettingSpec settingSpec);
}
