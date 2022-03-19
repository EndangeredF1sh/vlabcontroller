package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class UserLoginEvent extends ApplicationEvent {
    private final String userId;
    private final String sessionId;

    @Builder
    public UserLoginEvent(Object source, String userId, String sessionId) {
        super(source);
        this.userId = userId;
        this.sessionId = sessionId;
    }
}


