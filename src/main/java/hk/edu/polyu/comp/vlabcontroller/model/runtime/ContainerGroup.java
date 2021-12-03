package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ContainerSpec;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerGroup {
    @Getter @Setter private String id;
    @Getter @Setter private List<ContainerSpec> specs = new ArrayList<>();
    @Setter private Map<String, Object> parameters = new HashMap<>();

    @JsonIgnore public Map<String, Object> getParameters() {
        return parameters;
    }
}
