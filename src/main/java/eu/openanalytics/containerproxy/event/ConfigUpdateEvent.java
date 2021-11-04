package eu.openanalytics.containerproxy.event;

import org.springframework.context.ApplicationEvent;

public class ConfigUpdateEvent extends ApplicationEvent {
  public ConfigUpdateEvent(Object source) {
    super(source);
  }
}
