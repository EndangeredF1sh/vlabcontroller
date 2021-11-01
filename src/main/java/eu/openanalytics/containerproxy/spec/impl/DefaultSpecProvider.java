package eu.openanalytics.containerproxy.spec.impl;

import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.spec.IProxySpecProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "proxy")
public class DefaultSpecProvider implements IProxySpecProvider {
  @Getter @Setter private List<ProxySpec> specs = new ArrayList<>();
  
  public ProxySpec getSpec(String id) {
    if (id == null || id.isEmpty()) return null;
    return specs.stream().filter(s -> id.equals(s.getId())).findAny().orElse(null);
  }
}
