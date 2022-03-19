package hk.edu.polyu.comp.vlabcontroller.spec.expression;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ContainerSpec;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;

public class SpecExpressionContext {

    private ContainerSpec containerSpec;
    private ProxySpec proxySpec;
    private Proxy proxy;

    public static SpecExpressionContext create(Object... objects) {
        SpecExpressionContext ctx = new SpecExpressionContext();
        for (Object o : objects) {
            if (o instanceof ContainerSpec) {
                ctx.containerSpec = (ContainerSpec) o;
            } else if (o instanceof ProxySpec) {
                ctx.proxySpec = (ProxySpec) o;
            } else if (o instanceof Proxy) {
                ctx.proxy = (Proxy) o;
            }
        }
        return ctx;
    }

    public ContainerSpec getContainerSpec() {
        return containerSpec;
    }

    public ProxySpec getProxySpec() {
        return proxySpec;
    }

    public Proxy getProxy() {
        return proxy;
    }
}
