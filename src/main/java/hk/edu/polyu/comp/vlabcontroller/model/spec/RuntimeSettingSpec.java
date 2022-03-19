package hk.edu.polyu.comp.vlabcontroller.model.spec;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RuntimeSettingSpec {
    private String name;
    private String type;
    @Singular("config") private Map<String, Object> config = new HashMap<>();

    public RuntimeSettingSpec copy() {
        return this.toBuilder().build();
    }
}
