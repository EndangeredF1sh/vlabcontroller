package hk.edu.polyu.comp.vlabcontroller.stat.impl;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.event.*;
import hk.edu.polyu.comp.vlabcontroller.stat.IStatCollector;
import lombok.Setter;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.event.EventListener;

import javax.inject.Inject;
import java.io.IOException;

@RefreshScope
public abstract class AbstractDbCollector implements IStatCollector {
    @Setter(onMethod_ = {@Inject})
    protected ProxyProperties proxyProperties;

    @EventListener
    public void onUserLogoutEvent(UserLogoutEvent event) throws IOException {
        writeToDb(event.getTimestamp(), event.getUserId(), "Logout", null, String.valueOf(event.getWasExpired()));
    }

    @EventListener
    public void onUserLoginEvent(UserLoginEvent event) throws IOException {
        writeToDb(event.getTimestamp(), event.getUserId(), "Login", null, null);
    }

    @EventListener
    public void onProxyStartEvent(ProxyStartEvent event) throws IOException {
        writeToDb(event.getTimestamp(), event.getUserId(), "ProxyStart", event.getSpecId(), String.valueOf(event.getStartupTime().toMillis()));
    }

    @EventListener
    public void onProxyStopEvent(ProxyStopEvent event) throws IOException {
        writeToDb(event.getTimestamp(), event.getUserId(), "ProxyStop", event.getSpecId(), String.valueOf(event.getUsageTime().toMillis()));
    }

    @EventListener
    public void onProxyStartFailedEvent(ProxyStartFailedEvent event) throws IOException {
        // TODO
    }

    @EventListener
    public void onAuthFailedEvent(AuthFailedEvent event) throws IOException {
        // TODO
    }

    protected abstract void writeToDb(long timestamp, String userId, String type, String specId, String info) throws IOException;

}
