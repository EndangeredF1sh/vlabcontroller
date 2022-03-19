package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
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
    @Builder
    public UserLogoutEvent(Object source, String userId, String sessionId, Boolean wasExpired) {
        super(source);
        this.userId = userId;
        this.sessionId = sessionId;
        this.wasExpired = wasExpired;
    }
}


