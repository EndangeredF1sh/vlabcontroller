package hk.edu.polyu.comp.vlabcontroller.spec;

import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;

import java.util.List;

/**
 * A provider of base (predefined) ProxySpecs, e.g. from the application's configuration file.
 */
public interface IProxySpecProvider {

    List<ProxySpec> getSpecs();

    ProxySpec getSpec(String id);

}
