package hk.edu.polyu.comp.vlabcontroller.config;

import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.spec.IProxySpecProvider;
import hk.edu.polyu.comp.vlabcontroller.util.SessionHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
@RefreshScope
@Component
@Primary
@ConfigurationProperties(prefix = "proxy")
@AllArgsConstructor
@NoArgsConstructor
public class ProxyProperties implements IProxySpecProvider {
    List<String> adminGroups = new ArrayList<>();
    String allowedRole;
    String authentication = "none";
    String bindAddress = "0.0.0.0";
    String containerBackend = "kubernetes";
    String containerLogPath;
    String containerLogS3AccessKey;
    String containerLogS3AccessSecret;
    String containerLogS3Endpoint = "https://s3-eu-west-1.amazonaws.com";
    String domain;
    String faviconPath;
    String identifierLabel = "comp.polyu.edu.hk/vl-identifier";
    String identifierValue = "default-identifier";
    String landingPage = "/";
    String logoUrl;
    Duration maxAge = Duration.ofHours(4);
    String sameSiteCookie = "Lax";
    String templatePath;
    String title = "VLabController";
    boolean containerLogS3SSE = false;
    boolean heartbeatEnabled = true;
    boolean hideNavbar;
    String supportMailToAddress;
    int containerQuantityLimit = 2;
    Duration containerWaitTime = Duration.ofSeconds(20);
    Duration containerWaitTimeout = Duration.ofSeconds(5);
    Duration heartbeatRate = Duration.ofSeconds(10);
    Duration heartbeatTimeout = Duration.ofSeconds(60);
    Duration waitTimeout = Duration.ofSeconds(5);
    short port = 8080;
    List<ProxySpec> specs = new ArrayList<>();
    ProxySpec fileBrowser;
    String serviceName;

    @NestedConfigurationProperty ProxyKubernetesProperties kubernetes = new ProxyKubernetesProperties();
    @NestedConfigurationProperty ProxyKeycloakProperties keycloak = new ProxyKeycloakProperties();
    @NestedConfigurationProperty ProxyOpenIDProperties openID = new ProxyOpenIDProperties();
    @NestedConfigurationProperty ProxyOAuth2Properties oauth2 = new ProxyOAuth2Properties();
    @NestedConfigurationProperty ProxyWebServiceProperties webService = new ProxyWebServiceProperties();
    @NestedConfigurationProperty ProxyUsageStatsProperties usageStats = new ProxyUsageStatsProperties();
    @NestedConfigurationProperty ProxyEngagementProperties engagement = new ProxyEngagementProperties();
    @NestedConfigurationProperty List<ProxyUserProperties> users = new ArrayList<>();

    public ProxySpec getSpec(String id) {
        return Optional.ofNullable(id).filter(Predicate.not(String::isBlank))
            .flatMap(x -> specs.stream().filter(s -> x.equals(s.getId())).findAny())
            .orElse(null);
    }

    @PostConstruct
    public void afterPropertiesSet() {
        this.specs.stream().collect(Collectors.groupingBy(ProxySpec::getId)).forEach((id, duplicateSpecs) -> {
            if (duplicateSpecs.size() > 1)
                throw new IllegalArgumentException("Configuration error: spec with id '${id}' is defined multiple times");
        });
    }

    private static ServerProperties serverProperties;

    @Inject
    public void setServerProperties(ServerProperties serverProperties) {
        ProxyProperties.serverProperties = serverProperties;
    }

    public static String getPublicPath(String appName) {
        var contextPath = SessionHelper.getContextPath(serverProperties, true);
        return contextPath + "app_direct/" + appName + "/";
    }
}

