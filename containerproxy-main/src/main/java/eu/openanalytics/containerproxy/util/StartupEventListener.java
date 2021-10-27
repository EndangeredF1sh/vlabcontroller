package eu.openanalytics.containerproxy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StartupEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(StartupEventListener.class);
  
  @Inject
  private BuildProperties buildProperties;
  
  @EventListener
  public void onStartup(ApplicationReadyEvent event) {
    StringBuilder startupMsg = new StringBuilder("Started ");
    startupMsg.append(buildProperties.getName()).append(" ");
    startupMsg.append(buildProperties.getVersion()).append(" (");
    startupMsg.append("ContainerProxy ");
    startupMsg.append(buildProperties.get("containerProxyVersion")).append(")");
    LOGGER.info(startupMsg.toString());
  }
}
