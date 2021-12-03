package hk.edu.polyu.comp.vlabcontroller.model.spec;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class ResourceSpec {
    @Getter @Setter private Map<String, String> limits = new HashMap<>();
    @Getter @Setter private Map<String, String> requests = new HashMap<>();

}
