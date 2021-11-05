package eu.openanalytics.containerproxy.model.spec;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class RuntimeSettingSpec {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String type;
    @Getter
    @Setter
    private Map<String, Object> config;

    public void copy(RuntimeSettingSpec target) {
        target.setName(name);
        target.setType(type);
        if (config != null) {
            if (target.getConfig() == null) target.setConfig(new HashMap<>());
            target.getConfig().putAll(config);
        }
    }
}
