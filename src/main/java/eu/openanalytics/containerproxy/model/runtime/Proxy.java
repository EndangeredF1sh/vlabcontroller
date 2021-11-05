package eu.openanalytics.containerproxy.model.runtime;

import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private String userId;
    @Getter
    @Setter
    private String namespace;
    @Getter
    @Setter
    private List<Container> containers = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, URI> targets = new HashMap<>();
}
