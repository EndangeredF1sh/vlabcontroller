package eu.openanalytics.containerproxy.model.runtime;

import lombok.Getter;
import lombok.Setter;

public class RuntimeSetting {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private Object value;
}
