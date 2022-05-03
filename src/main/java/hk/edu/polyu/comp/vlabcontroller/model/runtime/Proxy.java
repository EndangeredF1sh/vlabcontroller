package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import lombok.*;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Data @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Proxy {
    private String id;
    private ProxySpec spec;
    private ProxyStatus status;
    private Duration startupTimestamp;
    private Duration createdTimestamp;
    private String userId;
    private boolean admin;
    private String namespace;
    private ContainerGroup containerGroup;
    @Builder.Default private Map<String, Object> metadata = new HashMap<>();
    @Singular private Map<String, URI> targets = new HashMap<>();
}
