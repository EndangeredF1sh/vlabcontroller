package hk.edu.polyu.comp.vlabcontroller.backend.strategy.impl;

import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyLogoutStrategy;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default logout behaviour: stop all proxies owned by the user.
 */
@Component
@ConditionalOnMissingBean(RedisSessionProxyLogoutStrategy.class)
public class DefaultProxyLogoutStrategy implements IProxyLogoutStrategy {
    private final ProxyService proxyService;

    public DefaultProxyLogoutStrategy(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @Override
    public void onLogout(String userId, boolean expired) {
        for (var proxy : proxyService.getProxies(p -> p.getUserId().equals(userId), true)) {
            proxyService.stopProxy(proxy, true, true, 0);
        }
    }

}
