package hk.edu.polyu.comp.vlabcontroller.service;

import hk.edu.polyu.comp.vlabcontroller.event.ConfigUpdateEvent;
import hk.edu.polyu.comp.vlabcontroller.util.ConfigFileHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@RefreshScope
@Service
@RequiredArgsConstructor
public class FileUpdateService {
    private final ConfigFileHelper configFileHelper;
    private final ApplicationEventPublisher publisher;
    private final ThreadPoolTaskScheduler taskScheduler;

    @Value("${proxy.config.interval:5000}")
    private int interval;

    @Value("${proxy.config.auto-update:true}")
    private boolean configAutoUpdate;

    private Optional<ScheduledFuture<?>> configUpdateFuture = Optional.empty();
    private String configHashCache;

    @PostConstruct
    public void start() {
        var self = this;
        if (configAutoUpdate) {
            log.info("Starting configuration auto detection, interval: {}ms", interval);
            configUpdateFuture.ifPresent(x -> x.cancel(true));
            configUpdateFuture = Optional.of(taskScheduler.scheduleAtFixedRate(() -> {
                var hash = configFileHelper.getConfigHash();
                if (configHashCache != null && !configHashCache.equals(hash)) {
                    publisher.publishEvent(new ConfigUpdateEvent(self));
                }
                configHashCache = hash;
            }, interval));
        }
    }
}
