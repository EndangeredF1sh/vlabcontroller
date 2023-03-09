package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Proxy {
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private ProxySpec spec;
    @Getter
    @Setter
    private ProxyStatus status;
    @Getter
    @Setter
    private long startupTimestamp;
    @Getter
    @Setter
    private long createdTimestamp;
    @Getter
    @Setter
    private long expirationTimestamp;
    @Getter
    @Setter
    private String userId;
    @Getter
    @Setter
    private boolean admin;
    @Getter
    @Setter
    private String namespace;
    @Getter
    @Setter
    private ContainerGroup containerGroup;
    @Getter
    @Setter
    private Map<String, URI> targets = new HashMap<>();
}
