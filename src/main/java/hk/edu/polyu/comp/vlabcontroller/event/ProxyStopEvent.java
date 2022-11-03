package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Duration;

public class ProxyStopEvent extends ApplicationEvent {
    @Getter
    private final String userId;
    @Getter
    private final String specId;
    @Getter
    private final Duration usageTime;
    @Getter
    private final String templateName;

    public ProxyStopEvent(Object source, String userId, String specId, Duration usageTime, String templateName) {
        super(source);
        this.userId = userId;
        this.specId = specId;
        this.usageTime = usageTime;
        this.templateName = templateName;
    }
}
