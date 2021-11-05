/**
 * ShinyProxy
 * <p>
 * Copyright (C) 2016-2021 Open Analytics
 * <p>
 * ===========================================================================
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 * <p>
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
package eu.openanalytics.shinyproxy;

import eu.openanalytics.containerproxy.service.HeartbeatService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

@Configuration
public class ShinyProxyConfiguration {

    private final HeartbeatService heartbeatService;
    private final Environment environment;

    public ShinyProxyConfiguration(@Lazy HeartbeatService heartbeatService, Environment environment) {
        this.heartbeatService = heartbeatService;
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        // Enable heartbeat unless explicitly disabled.
        boolean enabled = Boolean.valueOf(environment.getProperty("proxy.heartbeat-enabled", "true"));
        heartbeatService.setEnabled(enabled);
    }
}
