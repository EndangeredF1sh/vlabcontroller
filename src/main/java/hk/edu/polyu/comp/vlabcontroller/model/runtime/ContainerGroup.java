package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ContainerSpec;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ContainerGroup {
    private String id;
    @Singular private List<ContainerSpec> specs = new ArrayList<>();
    @Getter(onMethod_ = {@JsonIgnore}) @Singular private Map<String, Object> parameters = new HashMap<>();
}
