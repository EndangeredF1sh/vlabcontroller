package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.time.Duration;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class ProxyStartEvent extends ApplicationEvent {
    private final String proxyId;
    private final String userId;
    private final String specId;
    private final Duration startupTime;

    @Builder
    public ProxyStartEvent(Object source, String proxyId, String userId, String specId, Duration startupTime) {
        super(source);
        this.proxyId = proxyId;
        this.userId = userId;
        this.specId = specId;
        this.startupTime = startupTime;
    }
}
