package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import lombok.Builder;
import lombok.Data;

@Data @Builder(toBuilder = true)
public class RuntimeSetting {
    private String name;
    private Object value;
}
