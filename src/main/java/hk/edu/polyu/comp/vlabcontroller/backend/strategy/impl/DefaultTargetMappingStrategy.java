package hk.edu.polyu.comp.vlabcontroller.backend.strategy.impl;

import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyTargetMappingStrategy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ContainerGroup;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import org.springframework.stereotype.Component;

@Component
public class DefaultTargetMappingStrategy implements IProxyTargetMappingStrategy {

    public static final String DEFAULT_MAPPING_KEY = "default";

    public String createMapping(String mappingKey, ContainerGroup containerGroup, Proxy proxy) {
        var mapping = proxy.getId();
        if (!mappingKey.equalsIgnoreCase(DEFAULT_MAPPING_KEY)) {
            // For non-default mappings, also append the mapping key
            mapping += "/" + mappingKey;
        }
        return mapping;
    }

}
