package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class UserLoginEvent extends ApplicationEvent {
    @Getter
    private final String userId;
    @Getter
    private final String sessionId;

    public UserLoginEvent(Object source, String userId, String sessionId) {
        super(source);
        this.userId = userId;
        this.sessionId = sessionId;
    }
}


