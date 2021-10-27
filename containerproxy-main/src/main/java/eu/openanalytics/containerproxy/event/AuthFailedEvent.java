package eu.openanalytics.containerproxy.event;

import org.springframework.context.ApplicationEvent;

public class AuthFailedEvent extends ApplicationEvent {
  
  private final String userId;
  private final String sessionId;
  
  public AuthFailedEvent(Object source, String userId, String sessionId) {
    super(source);
    this.userId = userId;
    this.sessionId = sessionId;
  }
  
  public String getSessionId() {
    return sessionId;
  }
  
  public String getUserId() {
    return userId;
  }
  
}


