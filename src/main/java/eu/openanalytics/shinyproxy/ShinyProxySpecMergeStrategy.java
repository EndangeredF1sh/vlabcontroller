package eu.openanalytics.shinyproxy;

import eu.openanalytics.containerproxy.model.runtime.RuntimeSetting;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.spec.IProxySpecMergeStrategy;
import eu.openanalytics.containerproxy.spec.ProxySpecException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Primary
public class ShinyProxySpecMergeStrategy implements IProxySpecMergeStrategy {

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
