package hk.edu.polyu.comp.vlabcontroller.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupEventListener.class);

    private final BuildProperties buildProperties;

    public StartupEventListener(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @EventListener
    public void onStartup(ApplicationReadyEvent event) {
        StringBuilder startupMsg = new StringBuilder("Started ");
        startupMsg.append(buildProperties.getName()).append(" ");
        startupMsg.append(buildProperties.getVersion());
        LOGGER.info(startupMsg.toString());
    }
}
