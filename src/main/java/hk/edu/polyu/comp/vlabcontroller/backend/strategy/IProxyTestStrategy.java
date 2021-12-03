package hk.edu.polyu.comp.vlabcontroller.backend.strategy;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;

/**
 * Defines a strategy for testing the responsiveness of a newly launched proxy.
 * If a proxy is not responsive, the launch will be aborted and an error will be generated.
 */
public interface IProxyTestStrategy {

    boolean testProxy(Proxy proxy);

}
