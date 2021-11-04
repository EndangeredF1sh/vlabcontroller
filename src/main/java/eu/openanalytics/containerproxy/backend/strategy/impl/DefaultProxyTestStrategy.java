package eu.openanalytics.containerproxy.backend.strategy.impl;

import eu.openanalytics.containerproxy.backend.strategy.IProxyTestStrategy;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import org.springframework.stereotype.Component;

@Component
public class DefaultProxyTestStrategy implements IProxyTestStrategy {
  
  @Override
  public boolean testProxy(Proxy proxy) {
    return true;
  }
  
}
