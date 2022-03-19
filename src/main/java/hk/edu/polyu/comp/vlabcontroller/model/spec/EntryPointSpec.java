package hk.edu.polyu.comp.vlabcontroller.model.spec;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class EntryPointSpec {
    @Getter
    @Setter
    private String displayName;
    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
    private int port;
    @Getter
    @Setter
    private String path = "";
    @Getter
    @Setter
    private boolean disableSubdomain = false;
    @Getter
    @Setter
    private Map<String, String> parameters = new HashMap<>();
}
