package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class AuthFailedEvent extends ApplicationEvent {
    @Getter
    private final String userId;
    @Getter
    private final String sessionId;

    public AuthFailedEvent(Object source, String userId, String sessionId) {
        super(source);
        this.userId = userId;
        this.sessionId = sessionId;
    }
}


