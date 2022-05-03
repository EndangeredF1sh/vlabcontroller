package hk.edu.polyu.comp.vlabcontroller.stat.impl;

import hk.edu.polyu.comp.vlabcontroller.event.*;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.stat.IStatCollector;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Slf4j
public class Micrometer implements IStatCollector {
    @Setter(onMethod_ = {@Inject})
    private MeterRegistry registry;
    @Setter(onMethod_ = {@Inject})
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
        log.debug("UserLogoutEvent [user: {}, sessionId: {}, expired: {}]", event.getUserId(), event.getSessionId(), event.getWasExpired());
        userLogouts.increment();
        registry.counter("userIdLogouts", "user.id", event.getUserId()).increment();
    }

    @EventListener
    public void onUserLoginEvent(UserLoginEvent event) {
        log.debug("UserLoginEvent [user: {}, sessionId: {}]", event.getUserId(), event.getSessionId());
        userLogins.increment();
        registry.counter("userIdLogins", "user.id", event.getUserId()).increment();
        registry.counter("userIdLogouts", "user.id", event.getUserId()).increment(0);
    }

    @EventListener
    public void onProxyStartEvent(ProxyStartEvent event) {
        log.debug("ProxyStartEvent [user: {}, startupTime: {}]", event.getUserId(), event.getStartupTime());
        registry.counter("appStarts", "spec.id", event.getSpecId(), "user.id", event.getUserId()).increment();
        registry.timer("startupTime", "spec.id", event.getSpecId(), "user.id", event.getUserId()).record(event.getStartupTime());
    }

    @EventListener
    public void onProxyStopEvent(ProxyStopEvent event) {
        log.debug("ProxyStopEvent [user: {}, usageTime: {}]", event.getUserId(), event.getUsageTime());
        registry.counter("appStops", "spec.id", event.getSpecId(), "user.id", event.getUserId()).increment();
        registry.timer("usageTime", "spec.id", event.getSpecId(), "user.id", event.getUserId()).record(event.getUsageTime());
    }

    @EventListener
    public void onProxyStartFailedEvent(ProxyStartFailedEvent event) {
        log.debug("ProxyStartFailedEvent [user: {}, specId: {}]", event.getUserId(), event.getSpecId());
        appStartFailedCounter.increment();
    }

    @EventListener
    public void onAuthFailedEvent(AuthFailedEvent event) {
        log.debug("AuthFailedEvent [user: {}, sessionId: {}]", event.getUserId(), event.getSessionId());
        authFailedCounter.increment();
    }

}
