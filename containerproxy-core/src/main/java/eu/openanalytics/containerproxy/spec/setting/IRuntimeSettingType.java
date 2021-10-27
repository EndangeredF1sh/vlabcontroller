package eu.openanalytics.containerproxy.spec.setting;

import eu.openanalytics.containerproxy.model.runtime.RuntimeSetting;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.model.spec.RuntimeSettingSpec;
import eu.openanalytics.containerproxy.spec.ProxySpecException;

public interface IRuntimeSettingType {

	public void apply(RuntimeSetting setting, RuntimeSettingSpec settingSpec, ProxySpec targetSpec) throws ProxySpecException;

}
