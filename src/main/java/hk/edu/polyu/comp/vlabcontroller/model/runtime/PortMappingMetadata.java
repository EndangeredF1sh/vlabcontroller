package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URI;

@ToString
@AllArgsConstructor
public class PortMappingMetadata {
    @Getter @Setter private String portMapping;
    @Getter @Setter private URI target;
    @Getter @Setter private LoadBalancingProxyClient loadBalancingProxyClient;
}
