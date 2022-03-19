package hk.edu.polyu.comp.vlabcontroller.service;

import hk.edu.polyu.comp.vlabcontroller.event.ConfigUpdateEvent;
import hk.edu.polyu.comp.vlabcontroller.util.ConfigFileHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.NoSuchAlgorithmException;

@RefreshScope
@Service
public class FileUpdateService extends Thread {
    protected final Logger log = LogManager.getLogger(getClass());

    private final ConfigFileHelper configFileHelper;
    private final ApplicationEventPublisher publisher;

    @Value("${proxy.config.interval:5000}")
    private int interval;

    @Value("${proxy.config.auto-update:true}")
    private boolean configAutoUpdate;

    public FileUpdateService(ConfigFileHelper configFileHelper, ApplicationEventPublisher publisher) {
        this.configFileHelper = configFileHelper;
        this.publisher = publisher;
    }

    @PostConstruct
    public void start() {
        if (configAutoUpdate) {
            log.info("Starting configuration auto detection, interval: {}ms", interval);
            super.start();
        }
    }

    @Override
    public void run() {
        try {
            String before = configFileHelper.getConfigHash();
            while (true) {
                String after = configFileHelper.getConfigHash();
                if (!before.equals(after)) {
                    publisher.publishEvent(new ConfigUpdateEvent(this));
                }
                before = after;
                Thread.sleep(interval);
            }
        } catch (NoSuchAlgorithmException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
