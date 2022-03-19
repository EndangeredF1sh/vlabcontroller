package hk.edu.polyu.comp.vlabcontroller.spec.expression;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ContainerSpec;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import lombok.Getter;

public class SpecExpressionContext {
    @Getter
    private ContainerSpec containerSpec;
    @Getter
    private ProxySpec proxySpec;
    @Getter
    private Proxy proxy;

    public static SpecExpressionContext create(Object... objects) {
        var ctx = new SpecExpressionContext();
        for (var o : objects) {
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

}
