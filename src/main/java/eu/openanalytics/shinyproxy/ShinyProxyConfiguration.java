package eu.openanalytics.shinyproxy;

import eu.openanalytics.containerproxy.service.HeartbeatService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@Configuration
public class ShinyProxyConfiguration {

    private final HeartbeatService heartbeatService;
    private final Environment environment;

    public ShinyProxyConfiguration(@Lazy HeartbeatService heartbeatService, Environment environment) {
        this.heartbeatService = heartbeatService;
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        // Enable heartbeat unless explicitly disabled.
        boolean enabled = Boolean.valueOf(environment.getProperty("proxy.heartbeat-enabled", "true"));
        heartbeatService.setEnabled(enabled);
    }
}
