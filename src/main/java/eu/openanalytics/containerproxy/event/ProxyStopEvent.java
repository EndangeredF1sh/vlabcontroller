package eu.openanalytics.containerproxy.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Duration;

public class ProxyStopEvent extends ApplicationEvent {
  @Getter private final String userId;
  @Getter private final String specId;
  @Getter private final Duration usageTime;
  
  public ProxyStopEvent(Object source, String userId, String specId, Duration usageTime) {
    super(source);
    this.userId = userId;
    this.specId = specId;
    this.usageTime = usageTime;
  }
}
