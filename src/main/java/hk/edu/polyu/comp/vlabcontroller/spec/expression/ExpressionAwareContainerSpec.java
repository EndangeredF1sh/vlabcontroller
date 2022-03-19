package hk.edu.polyu.comp.vlabcontroller.spec.expression;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.spec.EntryPointSpec;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ContainerSpec;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ResourceSpec;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import org.springframework.data.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adds expression support to ContainerSpecs.
 * <p>
 * This means that the settings of a ContainerSpec may contain SpEL expressions, such as:
 * <pre>container-volumes: [ "/tmp/#{proxy.userId}/personalfolder:/var/personalfolder" ]</pre>
 * </p>
 * For more information on the expression language, see:
 * https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#expressions
 */
public class ExpressionAwareContainerSpec extends ContainerSpec {

    private final ContainerSpec source;
    private final SpecExpressionResolver resolver;
    private final SpecExpressionContext context;

    public ExpressionAwareContainerSpec(ContainerSpec source, Proxy proxy, SpecExpressionResolver resolver) {
        this.source = source;
        this.resolver = resolver;
        this.context = SpecExpressionContext.create(source, proxy, proxy.getSpec());
    }

    public String getImage() {
        return resolve(source.getImage());
    }

    public List<String> getCmd() {
        return resolve(source.getCmd());
    }

    public Map<String, String> getEnv() {
        Map<String, String> env = new HashMap<>();
        source.getEnv().entrySet().stream().forEach(e -> env.put(e.getKey(), resolve(e.getValue())));
        return env;
    }

    public String getEnvFile() {
        return resolve(source.getEnvFile());
    }

    public String getNetwork() {
        return resolve(source.getNetwork());
    }

    public List<String> getNetworkConnections() {
        return resolve(source.getNetworkConnections());
    }

    public List<String> getDns() {
        return resolve(source.getDns());
    }

    public List<EntryPointSpec> getEntryPoints() {
        return source.getEntryPoints();
    }

    public Map<String, Integer> getPortMapping() {
        return source.getPortMapping();
    }

    @Override
    public ResourceSpec getResources() {
        ResourceSpec resourceSpec = new ResourceSpec();
        source.getResources().getLimits().forEach((key, value) -> resourceSpec.getLimits().put(key, resolve(value)));
        source.getResources().getRequests().forEach((key, value) -> resourceSpec.getRequests().put(key, resolve(value)));
        return resourceSpec;
    }

    public boolean isPrivileged() {
        return source.isPrivileged();
    }

    @Override
    public Map<String, Pair<Boolean, String>> getRuntimeLabels() {
        // intentionally no SPeL applied!
        return source.getRuntimeLabels();
    }

    public Map<String, String> getSettings() {
        Map<String, String> settings = new HashMap<>();
        source.getSettings().entrySet().stream().forEach(e -> settings.put(e.getKey(), resolve(e.getValue())));
        return settings;
    }

    public List<VolumeMount> getVolumeMounts() {
        return source.getVolumeMounts().stream().map(volumeMount -> new VolumeMountBuilder()
            .withMountPath(resolve(volumeMount.getMountPath()))
            .withMountPropagation(resolve(volumeMount.getMountPropagation()))
            .withName(resolve(volumeMount.getName()))
            .withSubPath(resolve(volumeMount.getSubPath()))
            .withSubPathExpr(resolve(volumeMount.getSubPathExpr()))
            .withReadOnly(volumeMount.getReadOnly())
            .build()).collect(Collectors.toList());
    }

    @Override
    public List<VolumeMount> getAdminVolumeMounts() {
        return source.getAdminVolumeMounts().stream().map(volumeMount -> new VolumeMountBuilder()
                .withMountPath(resolve(volumeMount.getMountPath()))
                .withMountPropagation(resolve(volumeMount.getMountPropagation()))
                .withName(resolve(volumeMount.getName()))
                .withSubPath(resolve(volumeMount.getSubPath()))
                .withSubPathExpr(resolve(volumeMount.getSubPathExpr()))
                .withReadOnly(volumeMount.getReadOnly())
                .build()).collect(Collectors.toList());
    }

    protected String resolve(String expression) {
        if (expression == null) return null;
        return resolver.evaluateToString(expression, context);
    }

    protected List<String> resolve(List<String> expression) {
        return expression.stream()
                .map(x -> resolver.evaluateToString(x, context))
                .collect(Collectors.toList());
    }
}
