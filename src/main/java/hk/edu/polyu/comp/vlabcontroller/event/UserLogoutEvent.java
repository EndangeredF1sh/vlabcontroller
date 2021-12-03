package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class UserLogoutEvent extends ApplicationEvent {
    @Getter
    private final String userId;
    @Getter
    private final String sessionId;
    @Getter
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
}


