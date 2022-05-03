package hk.edu.polyu.comp.vlabcontroller;

import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.config.ServerProperties;
import hk.edu.polyu.comp.vlabcontroller.util.ProxyMappingManager;
import io.undertow.Handlers;
import io.undertow.servlet.api.ServletSessionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.redis.RedisHealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.filter.FormContentFilter;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties
@ComponentScan("hk.edu.polyu.comp")
@RequiredArgsConstructor
public class VLabControllerApplication {
    public static final String CONFIG_FILENAME = "application.yml";
    public static final String CONFIG_DEMO_PROFILE = "demo";
    private final Environment environment;
    private final ProxyMappingManager mappingManager;
    private final DefaultCookieSerializer defaultCookieSerializer;
    private final ServerProperties serverProps;
    private final ProxyProperties proxyProperties;

    public static void main(String[] args) {
        var app = new SpringApplication(VLabControllerApplication.class);

        var configFilename = Optional.ofNullable(System.getenv("SPRING_CONFIG_LOCATION"))
                .filter(Predicate.not(String::isBlank))
                .or(() -> Arrays.stream(args)
                        .filter(x -> x.contains("spring.config.location"))
                        .map(x -> x.split("=")[1]).findFirst())
                .orElse(CONFIG_FILENAME);
        if (!Files.exists(Paths.get(configFilename))) app.setAdditionalProfiles(CONFIG_DEMO_PROFILE);

        setDefaultProperties(app);

        try {
            app.setLogStartupInfo(false);
            app.run(args);
        } catch (Exception e) {
            // Workaround for bug in UndertowEmbeddedServletContainer.start():
            // If undertow.start() fails, started remains false which prevents undertow.stop() from ever being called.
            // Undertow's (non-daemon) XNIO worker threads will then prevent the JVM from exiting.
            if (e instanceof PortInUseException) System.exit(-1);
        }
    }

    private static void setDefaultProperties(SpringApplication app) {
        app.setDefaultProperties(new Properties() {{
            // use in-memory session storage by default. Can be overwritten in application.yml
            put("spring.session.store-type", "none");
            // required for proper working of the SP_USER_INITIATED_LOGOUT session attribute in the UserService
            put("spring.session.redis.flush-mode", "IMMEDIATE");

            // disable multi-part handling by Spring. We don't need this anywhere in the application.
            // When enabled this will cause problems when proxying file-uploads to apps.
            put("spring.servlet.multipart.enabled", "false");

            // disable logging of requests, since this reads part of the requests and therefore undertow is unable to correctly handle those requests
            put("logging.level.org.springframework.web.servlet.DispatcherServlet", "INFO");

            put("spring.application.name", "VLabController");

            // ====================
            // Metrics configuration
            // ====================

            // disable all supported exporters by default
            // Note: if we upgrade to Spring Boot 2.4.0 we can use put("management.metrics.export.defaults.enabled", "false");
            put("management.metrics.export.prometheus.enabled", "false");
            put("management.metrics.export.influx.enabled", "false");
            // set actuator to port 9090 (can be overwritten)
            put("management.server.port", "9090");
            // enable prometheus endpoint by default (but not the exporter)
            put("management.endpoint.prometheus.enabled", "true");
            // include prometheus and health endpoint in exposure
            put("management.endpoints.web.exposure.include", "health,prometheus");

            // ====================
            // Health configuration
            // ====================

            // enable redisSession check for the readiness probe
            put("management.endpoint.health.group.readiness.include", "readinessProbe,redisSession");
            // disable ldap health endpoint
            put("management.health.ldap.enabled", false);
            // disable default redis health endpoint since it's managed by redisSession
            put("management.health.redis.enabled", "false");
            // enable Kubernetes probes
            put("management.endpoint.health.probes.enabled", true);
        }});
        // See: https://github.com/keycloak/keycloak/pull/7053
        System.setProperty("jdk.serialSetFilterAfterRead", "true");
    }

    @PostConstruct
    public void init() {
        if (serverProps.isUseForwardHeaders()) {
            log.warn("WARNING: Using server.use-forward-headers will not work in this VLabController release, you need to change your configuration to use another property. See https://shinyproxy.io/documentation/security/#forward-headers on how to change your configuration.");
        }

        var sameSiteCookie = proxyProperties.getSameSiteCookie();
        log.debug("Setting sameSiteCookie policy to {}", sameSiteCookie);
        defaultCookieSerializer.setSameSite(sameSiteCookie);
        var proxyIdentifier = proxyProperties.getIdentifierValue();
        if (proxyIdentifier != null && !proxyIdentifier.isEmpty()) {
            defaultCookieSerializer.setCookieName("SESSION_" + proxyIdentifier.toUpperCase());
        }
    }

    // Disable specific Spring filters that parse the request body, preventing it from being proxied.

    @Bean
    public UndertowServletWebServerFactory servletContainer() {
        var factory = new UndertowServletWebServerFactory();
        factory.addDeploymentInfoCustomizers(info -> {
            info.setPreservePathOnForward(false); // required for the /api/route/{id}/ endpoint to work properly
            if (Boolean.parseBoolean(environment.getProperty("logging.requestdump", "false"))) {
                info.addOuterHandlerChainWrapper(Handlers::requestDump);
            }
            info.addInnerHandlerChainWrapper(mappingManager::createHttpHandler);
            var sessionConfig = new ServletSessionConfig();
            sessionConfig.setHttpOnly(true);
            sessionConfig.setSecure(serverProps.isSecureCookies());
            info.setServletSessionConfig(sessionConfig);
        });
        try {
            factory.setAddress(InetAddress.getByName(proxyProperties.getBindAddress()));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid bind address specified", e);
        }
        factory.setPort(proxyProperties.getPort());
        return factory;
    }

    @Bean
    public FilterRegistrationBean<FormContentFilter> registration2(FormContentFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Register the Jackson module which implements compatibility between javax.json and Jackson.
     *
     * @return
     */
    @Bean
    public JSR353Module jsr353Module() {
        return new JSR353Module();
    }

    @Bean
    public HealthIndicator redisSessionHealthIndicator(RedisConnectionFactory rdeRedisConnectionFactory) {
        if (Objects.equals(environment.getProperty("spring.session.store-type"), "redis")) {
            // if we are using redis for session -> use a proper health check for redis
            return new RedisHealthIndicator(rdeRedisConnectionFactory);
        } else {
            // not using redis for session -> just pretend it's always online
            return new HealthIndicator() {

                @Override
                public Health getHealth(boolean includeDetails) {
                    return Health.up().build();
                }

                @Override
                public Health health() {
                    return Health.up().build();
                }
            };
        }
    }

    /**
     * This Bean ensures that User Session are properly expired when using Redis for session storage.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.session.store-type", havingValue = "redis")
    public <S extends Session> SessionRegistry sessionRegistry(FindByIndexNameSessionRepository<S> sessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(sessionRepository);
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

}
