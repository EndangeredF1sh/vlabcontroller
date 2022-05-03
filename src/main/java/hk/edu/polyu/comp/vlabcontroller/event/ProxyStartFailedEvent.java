package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class ProxyStartFailedEvent extends ApplicationEvent {
    private final String userId;
    private final String specId;

    @Builder
    public ProxyStartFailedEvent(Object source, String userId, String specId) {
        super(source);
        this.userId = userId;
        this.specId = specId;
    }
}
