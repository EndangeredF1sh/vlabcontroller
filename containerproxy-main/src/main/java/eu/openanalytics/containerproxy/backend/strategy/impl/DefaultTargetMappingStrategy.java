package eu.openanalytics.containerproxy.backend.strategy.impl;

import eu.openanalytics.containerproxy.backend.strategy.IProxyTargetMappingStrategy;
import eu.openanalytics.containerproxy.model.runtime.Container;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import org.springframework.stereotype.Component;

@Component
public class DefaultTargetMappingStrategy implements IProxyTargetMappingStrategy {
  
  public static final String DEFAULT_MAPPING_KEY = "default";
  
  public String createMapping(String mappingKey, Container container, Proxy proxy) {
    String mapping = proxy.getId();
    if (!mappingKey.equalsIgnoreCase(DEFAULT_MAPPING_KEY)) {
      // For non-default mappings, also append the mapping key
      mapping += "/" + mappingKey;
    }
    return mapping;
  }
  
}
