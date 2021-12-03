package hk.edu.polyu.comp.vlabcontroller.backend.strategy.impl;

import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyTestStrategy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import org.springframework.stereotype.Component;

@Component
public class DefaultProxyTestStrategy implements IProxyTestStrategy {

    @Override
    public boolean testProxy(Proxy proxy) {
        return true;
    }

}
