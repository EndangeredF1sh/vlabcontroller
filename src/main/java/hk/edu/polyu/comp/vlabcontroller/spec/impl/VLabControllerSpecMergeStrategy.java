package hk.edu.polyu.comp.vlabcontroller.spec.impl;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.spec.IProxySpecMergeStrategy;
import hk.edu.polyu.comp.vlabcontroller.spec.ProxySpecException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Primary
public class VLabControllerSpecMergeStrategy implements IProxySpecMergeStrategy {

    @Override
    public ProxySpec merge(ProxySpec baseSpec, ProxySpec runtimeSpec, Set<RuntimeSetting> runtimeSettings) throws ProxySpecException {
        if (baseSpec == null) throw new ProxySpecException("Base proxy spec is required but missing");
        if (runtimeSpec != null) throw new ProxySpecException("Runtime proxy specs are not allowed");
        if (runtimeSettings != null && !runtimeSettings.isEmpty())
            throw new ProxySpecException("Runtime proxy settings are not allowed");

        ProxySpec finalSpec = new ProxySpec();
        baseSpec.copy(finalSpec);
        return finalSpec;
    }

}
