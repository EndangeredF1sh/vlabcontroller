package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import lombok.*;

import java.net.URI;

@ToString
@Data @Builder(toBuilder = true)
public class PortMappingMetadata {
    private String portMapping;
    private URI target;
    private LoadBalancingProxyClient loadBalancingProxyClient;
}
