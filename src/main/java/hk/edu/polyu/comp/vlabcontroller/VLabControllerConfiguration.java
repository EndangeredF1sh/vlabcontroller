package hk.edu.polyu.comp.vlabcontroller;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.service.HeartbeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
@RefreshScope
@EnableMongoRepositories
@EnableScheduling
public class VLabControllerConfiguration {
    private final HeartbeatService heartbeatService;
    private final ProxyProperties proxyProperties;
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @PostConstruct
    public void init() {
        threadPoolTaskScheduler.setPoolSize(2048);
        // Enable heartbeat unless explicitly disabled.
        heartbeatService.setEnabled(proxyProperties.isHeartbeatEnabled());
    }
}
