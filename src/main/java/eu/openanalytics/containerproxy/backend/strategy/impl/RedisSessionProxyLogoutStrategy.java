package eu.openanalytics.containerproxy.backend.strategy.impl;

import eu.openanalytics.containerproxy.backend.strategy.IProxyLogoutStrategy;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.util.RedisSessionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConditionalOnProperty(prefix = "spring.session", name = "store-type", havingValue = "redis")
public class RedisSessionProxyLogoutStrategy implements IProxyLogoutStrategy {
    private final Logger log = LogManager.getLogger(RedisSessionProxyLogoutStrategy.class);
    private final ProxyService proxyService;
    private final RedisSessionHelper redisSessionHelper;

    @Lazy
    public RedisSessionProxyLogoutStrategy(ProxyService proxyService, RedisSessionHelper redisSessionHelper) {
        this.proxyService = proxyService;
        this.redisSessionHelper = redisSessionHelper;
    }

    @PostConstruct
    private void init() {
        log.info("Enabled redis session logout strategy.");
    }

    @Override
    public void onLogout(String userId, boolean expired) {
        if (redisSessionHelper.getSessionByUsername(userId).size() > 1 - (expired ? 1 : 0)) {
            return;
        }
        for (Proxy proxy : proxyService.getProxies(p -> p.getUserId().equals(userId), true)) {
            proxyService.stopProxy(proxy, true, true);
        }
    }
}
