package hk.edu.polyu.comp.vlabcontroller.util;

import hk.edu.polyu.comp.vlabcontroller.event.ConfigUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.security.NoSuchAlgorithmException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ConfigUpdateListener {
    private final ConfigFileHelper configFileHelper;
    private final ContextRefresher contextRefresher;

    @EventListener
    public void onUpdate(ConfigUpdateEvent event) throws NoSuchAlgorithmException {
        var hash = configFileHelper.getConfigHash();
        if (hash.equals("unknown")) {
            log.info("No active application.yml set");
        } else if (hash.equals("illegal")) {
            log.error("application.yml syntax error");
        } else {
            log.info("Config changed, new hash = " + hash);
            new Thread(contextRefresher::refresh).start();
        }
    }

    @EventListener
    public void onOriginalEventPublished(EnvironmentChangeEvent event) {
        log.debug("Spring cloud env changed event published, " + event);
    }
}
