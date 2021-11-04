package eu.openanalytics.containerproxy.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class ProxyStartFailedEvent extends ApplicationEvent {
  @Getter private final String userId;
  @Getter private final String specId;
  
  public ProxyStartFailedEvent(Object source, String userId, String specId) {
    super(source);
    this.userId = userId;
    this.specId = specId;
  }
}
