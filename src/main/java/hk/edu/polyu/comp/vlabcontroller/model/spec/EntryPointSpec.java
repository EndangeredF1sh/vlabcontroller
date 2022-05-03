package hk.edu.polyu.comp.vlabcontroller.model.spec;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EntryPointSpec {
    private String displayName;
    private String description;
    private int port;
    @Builder.Default private String path = "";
    private boolean disableSubdomain;
    @Singular private Map<String, String> parameters = new HashMap<>();
}
