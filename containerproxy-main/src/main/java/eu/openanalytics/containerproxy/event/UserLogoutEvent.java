package eu.openanalytics.containerproxy.event;

import org.springframework.context.ApplicationEvent;

public class UserLogoutEvent extends ApplicationEvent {
  
  private final String userId;
  private final String sessionId;
  private final Boolean wasExpired;
  
  /**
   * @param source
   * @param userId
   * @param sessionId
   * @param wasExpired whether the user is logged autoamtically because the session has expired
   */
  public UserLogoutEvent(Object source, String userId, String sessionId, Boolean wasExpired) {
    super(source);
    this.userId = userId;
    this.sessionId = sessionId;
    this.wasExpired = wasExpired;
  }
  
  public String getSessionId() {
    return sessionId;
  }
  
  public String getUserId() {
    return userId;
  }
  
  public Boolean getWasExpired() {
    return wasExpired;
  }
}


