/**
 * ContainerProxy
 *
 * Copyright (C) 2016-2021 Open Analytics
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
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
import javax.inject.Inject;

@Component
@ConditionalOnProperty(prefix = "spring.session", name = "store-type", havingValue = "redis")
public class RedisSessionProxyLogoutStrategy implements IProxyLogoutStrategy {

    @Inject
    private ProxyService proxyService;

    @Inject
    @Lazy
    private RedisSessionHelper redisSessionHelper;

    private final Logger log = LogManager.getLogger(RedisSessionProxyLogoutStrategy.class);

    @PostConstruct
    private void init(){
        log.info("Enabled redis session logout strategy.");
    }

    @Override
    public void onLogout(String userId, boolean expired){
        if (redisSessionHelper.getSessionByUsername(userId).size() > 1 - (expired ? 1 : 0)) {
            return;
        }
        for (Proxy proxy: proxyService.getProxies(p -> p.getUserId().equals(userId), true)) {
            proxyService.stopProxy(proxy, true, true);
        }
    }
}
