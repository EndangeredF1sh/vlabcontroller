package hk.edu.polyu.comp.vlabcontroller.backend;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import hk.edu.polyu.comp.vlabcontroller.VLabControllerApplication;
import hk.edu.polyu.comp.vlabcontroller.VLabControllerException;
import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyTargetMappingStrategy;
import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyTestStrategy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ContainerGroup;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyStatus;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ContainerSpec;
import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import hk.edu.polyu.comp.vlabcontroller.spec.expression.ExpressionAwareContainerSpec;
import hk.edu.polyu.comp.vlabcontroller.spec.expression.SpecExpressionResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractContainerBackend implements IContainerBackend {

    protected static final String PROPERTY_INTERNAL_NETWORKING = "internal-networking";
    protected static final String PROPERTY_URL = "url";
    protected static final String PROPERTY_CERT_PATH = "cert-path";
    protected static final String PROPERTY_CONTAINER_PROTOCOL = "container-protocol";
    protected static final String PROPERTY_PRIVILEGED = "privileged";

    protected static final String DEFAULT_TARGET_PROTOCOL = "http";

    protected static final String ENV_VAR_USER_NAME = "VLAB_USERNAME";
    protected static final String ENV_VAR_USER_GROUPS = "VLAB_USERGROUPS";

    protected static final String RUNTIME_LABEL_PROXY_ID = "comp.polyu.edu.hk/vl-proxy-id";
    protected static final String RUNTIME_LABEL_USER_ID = "comp.polyu.edu.hk/vl-user-id";
    protected static final String RUNTIME_LABEL_USER_GROUPS = "comp.polyu.edu.hk/vl-user-groups";
    protected static final String RUNTIME_LABEL_PROXY_SPEC_ID = "comp.polyu.edu.hk/vl-spec-id";
    protected static final String RUNTIME_LABEL_CREATED_TIMESTAMP = "comp.polyu.edu.hk/vl-proxy-created-timestamp";
    protected static final String RUNTIME_LABEL_PROXIED_APP = "comp.polyu.edu.hk/vl-proxied-app";
    protected static final String RUNTIME_LABEL_INSTANCE = "comp.polyu.edu.hk/vl-instance";
    protected static final String RUNTIME_LABEL_TEMPLATE_NAME = "comp.polyu.edu.hk/vl-template-name";

    protected final Logger log = LogManager.getLogger(getClass());
    @Inject
    protected IProxyTargetMappingStrategy mappingStrategy;
    @Inject
    protected IProxyTestStrategy testStrategy;
    @Inject
    protected UserService userService;
    @Inject
    protected Environment environment;
    @Inject
    protected SpecExpressionResolver expressionResolver;
    @Inject
    @Lazy
    // Note: lazy needed to work around early initialization conflict
    protected IAuthenticationBackend authBackend;
    protected String instanceId = null;
    private boolean useInternalNetwork;
    private boolean privileged;

    @Override
    public void initialize() throws VLabControllerException {
        // If this application runs as a container itself, things like port publishing can be omitted.
        useInternalNetwork = Boolean.parseBoolean(getProperty(PROPERTY_INTERNAL_NETWORKING, "false"));
        privileged = Boolean.parseBoolean(getProperty(PROPERTY_PRIVILEGED, "false"));
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
        proxy.setCreatedTimestamp(System.currentTimeMillis());

        try {
            try {
                doStartProxy(proxy);
            } catch (Throwable t) {
                throw new VLabControllerException("Failed to start container", t);
            }

            if (!testStrategy.testProxy(proxy)) {
                throw new VLabControllerException("Container did not respond in time");
            }

            long currentTimestamp = System.currentTimeMillis();
            long maxAgeDurationMs = Duration.parse(environment.getProperty("proxy.engagement.max-age", "PT4H")).toMillis();
            proxy.setStartupTimestamp(currentTimestamp);
            proxy.setExpirationTimestamp(currentTimestamp + maxAgeDurationMs);
            proxy.setStatus(ProxyStatus.Up);

        } catch (VLabControllerException e) {
            try {
                stopProxy(proxy);
            } catch (Exception ex) {
                log.error(ex);
            }
            throw e;
        }
    }

    protected void doStartProxy(Proxy proxy) throws Exception {
        var eSpecs = proxy.getSpec().getContainerSpecs().stream()
        .map(spec -> {
            if (authBackend != null) authBackend.customizeContainer(spec);

            // add labels need for App Recovery and maintenance
            spec.addRuntimeLabel(RUNTIME_LABEL_PROXIED_APP, true, "true");
            spec.addRuntimeLabel(RUNTIME_LABEL_INSTANCE, true, instanceId);
            spec.addRuntimeLabel(RUNTIME_LABEL_TEMPLATE_NAME, true, proxy.getSpec().getTemplateName().replaceAll("\\s+", "_"));

            spec.addRuntimeLabel(RUNTIME_LABEL_PROXY_ID, true, proxy.getId());
            spec.addRuntimeLabel(RUNTIME_LABEL_PROXY_SPEC_ID, true, proxy.getSpec().getId());
            spec.addRuntimeLabel(RUNTIME_LABEL_USER_ID, true, proxy.getUserId());
            spec.addRuntimeLabel(RUNTIME_LABEL_CREATED_TIMESTAMP, true, String.valueOf(proxy.getCreatedTimestamp()));
            String[] groups = userService.getGroups(userService.getCurrentAuth());
            spec.addRuntimeLabel(RUNTIME_LABEL_USER_GROUPS, false, String.join(",", groups));

            return new ExpressionAwareContainerSpec(spec, proxy, expressionResolver);
        })
        .map(ContainerSpec.class::cast)
        .collect(Collectors.toList());

        ContainerGroup c = startContainer(eSpecs, proxy);
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

    protected String getProperty(String key) {
        return getProperty(key, null);
    }

    protected String getProperty(String key, String defaultValue) {
        return environment.getProperty(getPropertyPrefix() + key, defaultValue);
    }

    protected abstract String getPropertyPrefix();

    protected Long memoryToBytes(String memory) {
        if (memory == null || memory.isEmpty()) return null;
        Matcher matcher = Pattern.compile("(\\d+)([bkmg]?)").matcher(memory.toLowerCase());
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid memory argument: " + memory);
        long mem = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        switch (unit) {
            case "k":
                mem *= 1024;
                break;
            case "m":
                mem *= 1024 * 1024;
                break;
            case "g":
                mem *= 1024 * 1024 * 1024;
                break;
            default:
        }
        return mem;
    }

    protected List<String> buildEnv(ContainerSpec containerSpec, Proxy proxy) throws IOException {
        List<String> env = new ArrayList<>();
        env.add(String.format("%s=%s", ENV_VAR_USER_NAME, proxy.getUserId()));

        String[] groups = userService.getGroups(userService.getCurrentAuth());
        env.add(String.format("%s=%s", ENV_VAR_USER_GROUPS, String.join(",", groups)));

        String envFile = containerSpec.getEnvFile();
        if (envFile != null && Files.isRegularFile(Paths.get(envFile))) {
            Properties envProps = new Properties();
            envProps.load(new FileInputStream(envFile));
            for (Map.Entry<Object, Object> key : envProps.entrySet()) {
                env.add(String.format("%s=%s", key.getKey(), key.getValue()));
            }
        }

        if (containerSpec.getEnv() != null) {
            for (Map.Entry<String, String> entry : containerSpec.getEnv().entrySet()) {
                env.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
            }
        }

        // Allow the authentication backend to add values to the environment, if needed.
        if (authBackend != null) authBackend.customizeContainerEnv(env);

        return env;
    }

    protected boolean isUseInternalNetwork() {
        return useInternalNetwork;
    }

    protected boolean isPrivileged() {
        return privileged;
    }


    private File getPathToConfigFile() {
        String path = environment.getProperty("spring.config.location");
        if (path != null) {
            return Paths.get(path).toFile();
        }

        File file = Paths.get(VLabControllerApplication.CONFIG_FILENAME).toFile();
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
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        File file = getPathToConfigFile();
        if (file == null) {
            // this should only happen in tests
            instanceId = "unknown-instance-id";
            return instanceId;
        }

        Object parsedConfig = objectMapper.readValue(file, Object.class);
        String canonicalConfigFile = objectMapper.writeValueAsString(parsedConfig);

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(canonicalConfigFile.getBytes(Charsets.UTF_8));
        instanceId = String.format("%040x", new BigInteger(1, digest.digest()));
        return instanceId;
    }
}
