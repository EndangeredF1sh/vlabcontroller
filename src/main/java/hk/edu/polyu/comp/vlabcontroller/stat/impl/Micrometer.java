package hk.edu.polyu.comp.vlabcontroller.stat.impl;

import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.stat.IStatCollector;
import hk.edu.polyu.comp.vlabcontroller.event.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class Micrometer implements IStatCollector {

    private final Logger logger = LogManager.getLogger(getClass());
    @Inject
    private MeterRegistry registry;
    @Inject
    private ProxyService proxyService;
    private Counter appStartFailedCounter;

    private Counter authFailedCounter;

    private Counter userLogins;

    private Counter userLogouts;

    @PostConstruct
    public void init() {

        userLogins = registry.counter("userLogins");
        userLogouts = registry.counter("userLogouts");
        appStartFailedCounter = registry.counter("startFailed");
        authFailedCounter = registry.counter("authFailed");

    }

    @EventListener
    public void onUserLogoutEvent(UserLogoutEvent event) {
        logger.debug("UserLogoutEvent [user: {}, sessionId: {}, expired: {}]", event.getUserId(), event.getSessionId(), event.getWasExpired());
        userLogouts.increment();
        registry.counter("userIdLogouts", "user.id", event.getUserId()).increment();
    }

    @EventListener
    public void onUserLoginEvent(UserLoginEvent event) {
        logger.debug("UserLoginEvent [user: {}, sessionId: {}]", event.getUserId(), event.getSessionId());
        userLogins.increment();
        registry.counter("userIdLogins", "user.id", event.getUserId()).increment();
        registry.counter("userIdLogouts", "user.id", event.getUserId()).increment(0);
    }

    @EventListener
    public void onProxyStartEvent(ProxyStartEvent event) {
        logger.debug("ProxyStartEvent [user: {}, startupTime: {}]", event.getUserId(), event.getStartupTime());
        registry.counter("appStarts", "spec.id", event.getSpecId(), "user.id", event.getUserId()).increment();
        registry.timer("startupTime", "spec.id", event.getSpecId(), "user.id", event.getUserId()).record(event.getStartupTime());
    }

    @EventListener
    public void onProxyStopEvent(ProxyStopEvent event) {
        logger.debug("ProxyStopEvent [user: {}, usageTime: {}]", event.getUserId(), event.getUsageTime());
        registry.counter("appStops", "spec.id", event.getSpecId(), "user.id", event.getUserId()).increment();
        registry.timer("usageTime", "spec.id", event.getSpecId(), "user.id", event.getUserId()).record(event.getUsageTime());
    }

    @EventListener
    public void onProxyStartFailedEvent(ProxyStartFailedEvent event) {
        logger.debug("ProxyStartFailedEvent [user: {}, specId: {}]", event.getUserId(), event.getSpecId());
        appStartFailedCounter.increment();
    }

    @EventListener
    public void onAuthFailedEvent(AuthFailedEvent event) {
        logger.debug("AuthFailedEvent [user: {}, sessionId: {}]", event.getUserId(), event.getSessionId());
        authFailedCounter.increment();
    }

}
