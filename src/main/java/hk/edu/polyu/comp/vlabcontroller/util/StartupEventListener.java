package hk.edu.polyu.comp.vlabcontroller.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupEventListener {
    private final BuildProperties buildProperties;

    public StartupEventListener(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @EventListener
    public void onStartup(ApplicationReadyEvent event) {
        log.info(String.format("Started %s %s", buildProperties.getName(), buildProperties.getVersion()));
    }
}
