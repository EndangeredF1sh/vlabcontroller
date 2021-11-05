package eu.openanalytics.containerproxy.spec.expression;

import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.model.spec.ContainerSpec;
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
        if (source.getEnv() == null) return null;
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

    public List<String> getVolumes() {
        return resolve(source.getVolumes());
    }

    public Map<String, Integer> getPortMapping() {
        return source.getPortMapping();
    }

    public String getMemoryRequest() {
        return resolve(source.getMemoryRequest());
    }

    public String getMemoryLimit() {
        return resolve(source.getMemoryLimit());
    }

    public String getCpuRequest() {
        return resolve(source.getCpuRequest());
    }

    public String getCpuLimit() {
        return resolve(source.getCpuLimit());
    }

    public boolean isPrivileged() {
        return source.isPrivileged();
    }

    @Override
    public Map<String, String> getLabels() {
        if (source.getLabels() == null) return null;
        Map<String, String> settings = new HashMap<>();
        source.getLabels().entrySet().stream().forEach(e -> settings.put(e.getKey(), resolve(e.getValue())));
        return settings;
    }

    @Override
    public Map<String, Pair<Boolean, String>> getRuntimeLabels() {
        // intentionally no SPeL applied!
        return source.getRuntimeLabels();
    }

    public Map<String, String> getSettings() {
        if (source.getSettings() == null) return null;
        Map<String, String> settings = new HashMap<>();
        source.getSettings().entrySet().stream().forEach(e -> settings.put(e.getKey(), resolve(e.getValue())));
        return settings;
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
