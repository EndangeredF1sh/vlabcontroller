package eu.openanalytics.containerproxy.spec.impl;

import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.spec.IProxySpecProvider;
import eu.openanalytics.containerproxy.util.SessionHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Primary
@ConfigurationProperties(prefix = "proxy")
public class DefaultSpecProvider implements IProxySpecProvider {
    private static Environment environment;
    @Getter
    @Setter
    private List<ProxySpec> specs = new ArrayList<>();

    public static String getPublicPath(String appName) {
        String contextPath = SessionHelper.getContextPath(environment, true);
        return contextPath + "app_direct/" + appName + "/";
    }

    public ProxySpec getSpec(String id) {
        if (id == null || id.isEmpty()) return null;
        return specs.stream().filter(s -> id.equals(s.getId())).findAny().orElse(null);
    }

    @PostConstruct
    public void afterPropertiesSet() {
        this.specs.stream().collect(Collectors.groupingBy(ProxySpec::getId)).forEach((id, duplicateSpecs) -> {
            if (duplicateSpecs.size() > 1)
                throw new IllegalArgumentException(String.format("Configuration error: spec with id '%s' is defined multiple times", id));
        });
    }

    @Autowired
    public void setEnvironment(Environment env) {
        DefaultSpecProvider.environment = env;
    }
}
