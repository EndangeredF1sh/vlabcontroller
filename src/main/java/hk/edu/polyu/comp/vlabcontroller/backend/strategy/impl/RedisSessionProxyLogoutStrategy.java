package hk.edu.polyu.comp.vlabcontroller.backend.strategy.impl;

import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyLogoutStrategy;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.util.RedisSessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "spring.session", name = "store-type", havingValue = "redis")
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class RedisSessionProxyLogoutStrategy implements IProxyLogoutStrategy {
    private final ProxyService proxyService;
    private final RedisSessionHelper redisSessionHelper;

    @PostConstruct
    private void init() {
        log.info("Enabled redis session logout strategy.");
    }

    @Override
    public void onLogout(String userId, boolean expired) {
        if (redisSessionHelper.getSessionByUsername(userId).size() > 1 - (expired ? 1 : 0)) {
            return;
        }
        proxyService.getProxies(p -> p.getUserId().equals(userId), true).forEach(proxy -> proxyService.stopProxy(proxy, true, true, Duration.ZERO));
    }
}
