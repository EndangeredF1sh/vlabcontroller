package eu.openanalytics.containerproxy.model.spec;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class SubApplicationSpec {
    @Getter
    @Setter
    private String path = "";
    @Getter
    @Setter
    private String subDomain = "";
    @Getter
    @Setter
    private String displayName;
    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
    private Map<String, String> parameters = new HashMap<>();
}
