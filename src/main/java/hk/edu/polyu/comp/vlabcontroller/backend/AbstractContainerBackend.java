package hk.edu.polyu.comp.vlabcontroller.backend;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Maps;
import hk.edu.polyu.comp.vlabcontroller.VLabControllerApplication;
import hk.edu.polyu.comp.vlabcontroller.VLabControllerException;
import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyTargetMappingStrategy;
import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyTestStrategy;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ContainerGroup;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyStatus;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ContainerSpec;
import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import hk.edu.polyu.comp.vlabcontroller.spec.expression.ExpressionAwareContainerSpec;
import hk.edu.polyu.comp.vlabcontroller.spec.expression.SpecExpressionResolver;
import io.vavr.Function1;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static io.vavr.API.unchecked;

@Slf4j
public abstract class AbstractContainerBackend implements IContainerBackend {
    protected static final String ENV_VAR_USER_NAME = "VLAB_USERNAME";
    protected static final String ENV_VAR_USER_GROUPS = "VLAB_USERGROUPS";

    protected static final String RUNTIME_LABEL_PROXY_ID = "comp.polyu.edu.hk/vl-proxy-id";
    protected static final String RUNTIME_LABEL_USER_ID = "comp.polyu.edu.hk/vl-user-id";
    protected static final String RUNTIME_LABEL_USER_GROUPS = "comp.polyu.edu.hk/vl-user-groups";
    protected static final String RUNTIME_LABEL_PROXY_SPEC_ID = "comp.polyu.edu.hk/vl-spec-id";
    protected static final String RUNTIME_LABEL_CREATED_TIMESTAMP = "comp.polyu.edu.hk/vl-proxy-created-timestamp";
    protected static final String RUNTIME_LABEL_PROXIED_APP = "comp.polyu.edu.hk/vl-proxied-app";
    protected static final String RUNTIME_LABEL_INSTANCE = "comp.polyu.edu.hk/vl-instance";
    protected static final String RUNTIME_LABEL_EVALUATOR = "comp.polyu.edu.hk/is-evaluator";

    @Setter(onMethod_ = {@Inject})
    protected IProxyTargetMappingStrategy mappingStrategy;
    @Setter(onMethod_ = {@Inject})
    protected IProxyTestStrategy testStrategy;
    @Setter(onMethod_ = {@Inject})
    protected UserService userService;
    @Setter(onMethod_ = {@Inject})
    protected Environment environment;
    @Setter(onMethod_ = {@Inject})
    protected SpecExpressionResolver expressionResolver;
    @Setter(onMethod_ = {@Inject, @Lazy})
    // Note: lazy needed to work around early initialization conflict
    protected IAuthenticationBackend authBackend;
    @Setter(onMethod_ = {@Inject})
    protected ProxyProperties proxyProperties;

    protected String instanceId = null;


    @Override
    public void initialize() throws VLabControllerException {
        try {
            instanceId = calculateInstanceId();
            log.info("Hash of config is: " + instanceId);
        } catch (Exception e) {
            throw new RuntimeException("Cannot compute hash of config", e);
        }
    }

    @Override
    public void startProxy(Proxy proxy) throws VLabControllerException {
        proxy.setId(UUID.randomUUID().toString());
        proxy.setStatus(ProxyStatus.Starting);
        proxy.setCreatedTimestamp(Duration.ofMillis(System.currentTimeMillis()));

        try {
            try {
                doStartProxy(proxy);
            } catch (Throwable t) {
                throw new VLabControllerException("Failed to start container", t);
            }

            if (!testStrategy.testProxy(proxy)) {
                throw new VLabControllerException("Container did not respond in time");
            }

            proxy.setStartupTimestamp(Duration.ofMillis(System.currentTimeMillis()));
            proxy.setStatus(ProxyStatus.Up);

        } catch (VLabControllerException e) {
            try {
                stopProxy(proxy);
            } catch (Exception ex) {
                log.error("an error occured: {}", ex);
            }
            throw e;
        }
    }

    protected void doStartProxy(Proxy proxy) throws Exception {
        Function1<ContainerSpec, ContainerSpec> applySpecToProxy = spec -> {
            if (authBackend != null) authBackend.customizeContainer(spec);

            // add labels need for App Recovery and maintenance
            spec.addRuntimeLabel(RUNTIME_LABEL_INSTANCE, true, instanceId);
            spec.addRuntimeLabel(RUNTIME_LABEL_PROXY_ID, true, proxy.getId());
            spec.addRuntimeLabel(RUNTIME_LABEL_PROXY_SPEC_ID, true, proxy.getSpec().getId());
            spec.addRuntimeLabel(RUNTIME_LABEL_USER_ID, true, proxy.getUserId());
            spec.addRuntimeLabel(RUNTIME_LABEL_CREATED_TIMESTAMP, true, String.valueOf(proxy.getCreatedTimestamp()));
            var groups = userService.getGroups(userService.getCurrentAuth());
            spec.addRuntimeLabel(RUNTIME_LABEL_USER_GROUPS, false, String.join(",", groups));

            return (ContainerSpec) new ExpressionAwareContainerSpec(spec, proxy, expressionResolver);
        };

        var eSpecs = proxy.getSpec().getContainerSpecs().stream()
            .map(applySpecToProxy)
            .peek(spec -> spec.addRuntimeLabel(RUNTIME_LABEL_PROXIED_APP, true, "true"))
            .collect(Collectors.toList());
        Optional.ofNullable(proxy.getSpec().getEvaluator()).ifPresent(evaluator -> {
            var spec = applySpecToProxy.apply(evaluator);
            spec.addRuntimeLabel(RUNTIME_LABEL_EVALUATOR, true, "true");
            spec.getEnv().put("CONTROLLER_HOST", proxyProperties.getServiceName());
            spec.addRuntimeLabel(RUNTIME_LABEL_EVALUATOR, true, "true");
            eSpecs.add(spec);
        });
        var c = startContainer(eSpecs, proxy);
        proxy.setContainerGroup(c);
    }

    protected abstract ContainerGroup startContainer(List<ContainerSpec> spec, Proxy proxy) throws Exception;

    @Override
    public void stopProxy(Proxy proxy) throws VLabControllerException {
        try {
            proxy.setStatus(ProxyStatus.Stopping);
            doStopProxy(proxy);
            proxy.setStatus(ProxyStatus.Stopped);
        } catch (Exception e) {
            throw new VLabControllerException("Failed to stop container", e);
        }
    }

    protected abstract void doStopProxy(Proxy proxy) throws Exception;

    @Override
    public BiConsumer<OutputStream, OutputStream> getOutputAttacher(Proxy proxy) {
        // Default: do not support output attaching.
        return null;
    }

    protected Map<String, String> buildEnv(ContainerSpec containerSpec, Proxy proxy) {
        return new HashMap<>() {{
            put(ENV_VAR_USER_NAME, proxy.getUserId());
            put(ENV_VAR_USER_GROUPS, String.join(",", userService.getGroups(userService.getCurrentAuth())));
            Optional.ofNullable(containerSpec.getEnvFile())
                .filter(x -> Files.isRegularFile(Paths.get(x)))
                .map(unchecked(FileInputStream::new))
                .map(unchecked(x -> new Properties() {{ load(x); }}))
                .map(Maps::fromProperties)
                .ifPresent(this::putAll);
            Optional.ofNullable(containerSpec.getEnv()).ifPresent(this::putAll);
            // Allow the authentication backend to add values to the environment, if needed.
            Optional.ofNullable(authBackend).ifPresent(x -> x.customizeContainerEnv(this));
        }};
    }

    private File getPathToConfigFile() {
        var path = environment.getProperty("spring.config.location");
        if (path != null) {
            return Paths.get(path).toFile();
        }

        var file = Paths.get(VLabControllerApplication.CONFIG_FILENAME).toFile();
        if (file.exists()) {
            return file;
        }

        return null;
    }

    /**
     * Calculates a hash of the config file (i.e. application.yaml).
     */
    private String calculateInstanceId() throws IOException, NoSuchAlgorithmException {
        /**
         * We need a hash of some "canonical" version of the config file.
         * The hash should not change when e.g. comments are added to the file.
         * Therefore we read the application.yml file into an Object and then
         * dump it again into YAML. We also sort the keys of maps and properties so that
         * the order does not matter for the resulting hash.
         */
        var objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        var file = getPathToConfigFile();
        if (file == null) {
            // this should only happen in tests
            instanceId = "unknown-instance-id";
            return instanceId;
        }

        var parsedConfig = objectMapper.readValue(file, Object.class);
        var canonicalConfigFile = objectMapper.writeValueAsString(parsedConfig);

        var digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(canonicalConfigFile.getBytes(StandardCharsets.UTF_8));
        instanceId = String.format("%040x", new BigInteger(1, digest.digest()));
        return instanceId;
    }
}
