package hk.edu.polyu.comp.vlabcontroller.backend.strategy.impl;

import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyLogoutStrategy;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Default logout behaviour: stop all proxies owned by the user.
 */
@Component
@ConditionalOnMissingBean(RedisSessionProxyLogoutStrategy.class)
@RequiredArgsConstructor
public class DefaultProxyLogoutStrategy implements IProxyLogoutStrategy {
    private final ProxyService proxyService;

    @Override
    public void onLogout(String userId, boolean expired) {
        proxyService.getProxies(p -> p.getUserId().equals(userId), true).forEach(proxy -> proxyService.stopProxy(proxy, true, true, Duration.ZERO));
    }

}
