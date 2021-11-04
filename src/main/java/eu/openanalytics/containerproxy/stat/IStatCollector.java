package eu.openanalytics.containerproxy.stat;

import eu.openanalytics.containerproxy.event.*;
import org.springframework.context.event.EventListener;

import java.io.IOException;

public interface IStatCollector {
  
  @EventListener
  default void onUserLogoutEvent(UserLogoutEvent event) throws IOException {
  }
  
  @EventListener
  default void onUserLoginEvent(UserLoginEvent event) throws IOException {
  }
  
  @EventListener
  default void onProxyStartEvent(ProxyStartEvent event) throws IOException {
  }
  
  @EventListener
  default void onProxyStopEvent(ProxyStopEvent event) throws IOException {
  }
  
  @EventListener
  default void onProxyStartFailedEvent(ProxyStartFailedEvent event) throws IOException {
  }
  
  @EventListener
  default void onAuthFailedEvent(AuthFailedEvent event) throws IOException {
  }
  
}
