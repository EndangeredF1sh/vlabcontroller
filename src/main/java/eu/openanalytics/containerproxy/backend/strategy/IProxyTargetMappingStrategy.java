package eu.openanalytics.containerproxy.backend.strategy;

import eu.openanalytics.containerproxy.model.runtime.Container;
import eu.openanalytics.containerproxy.model.runtime.Proxy;

/**
 * Defines a strategy for creating a concrete mapping (a piece of URL) using the mapping key provided by the container spec.
 * <p>
 * For example, if the mapping spec is { "mymapping": 8080 } then the mapping strategy may be computed as
 * "/containerId/mymapping" which would map into port 8080 of the container.
 */
public interface IProxyTargetMappingStrategy {

    String createMapping(String mappingKey, Container container, Proxy proxy);

}
