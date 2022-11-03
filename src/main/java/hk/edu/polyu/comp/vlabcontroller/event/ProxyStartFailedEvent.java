package hk.edu.polyu.comp.vlabcontroller.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class ProxyStartFailedEvent extends ApplicationEvent {
    @Getter
    private final String userId;
    @Getter
    private final String specId;
    @Getter
    private final String templateName;

    public ProxyStartFailedEvent(Object source, String userId, String specId, String templateName) {
        super(source);
        this.userId = userId;
        this.specId = specId;
        this.templateName = templateName;
    }
}
