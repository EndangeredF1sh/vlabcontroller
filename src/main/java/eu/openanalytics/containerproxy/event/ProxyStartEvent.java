package eu.openanalytics.containerproxy.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Duration;

public class ProxyStartEvent extends ApplicationEvent {
    @Getter
    private final String userId;
    @Getter
    private final String specId;
    @Getter
    private final Duration startupTime;

    public ProxyStartEvent(Object source, String userId, String specId, Duration startupTime) {
        super(source);
        this.userId = userId;
        this.specId = specId;
        this.startupTime = startupTime;
    }
}
