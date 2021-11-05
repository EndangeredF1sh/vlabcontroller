package eu.openanalytics.containerproxy.util;

import eu.openanalytics.containerproxy.event.ConfigUpdateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.security.NoSuchAlgorithmException;

@Configuration
public class ConfigUpdateListener {
    protected final Logger log = LogManager.getLogger(getClass());

    private final ConfigFileHelper configFileHelper;
    private final ContextRefresher contextRefresher;

    public ConfigUpdateListener(ConfigFileHelper configFileHelper, ContextRefresher contextRefresher) {
        this.configFileHelper = configFileHelper;
        this.contextRefresher = contextRefresher;
    }

    @EventListener
    public void onUpdate(ConfigUpdateEvent event) throws NoSuchAlgorithmException {
        String hash = configFileHelper.getConfigHash();
        if (hash.equals("unknown")) {
            log.info("No active application.yml set");
        } else if (hash.equals("illegal")) {
            log.error("application.yml syntax error");
        } else {
            log.info("Config changed, new hash = " + hash);
            new Thread(() -> contextRefresher.refresh()).start();
        }
    }

    @EventListener
    public void onOriginalEventPublished(EnvironmentChangeEvent event) {
        log.debug("Spring cloud env changed event published, " + event);
    }
}
