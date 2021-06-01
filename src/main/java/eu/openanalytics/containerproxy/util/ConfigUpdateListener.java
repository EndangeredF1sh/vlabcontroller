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

package eu.openanalytics.containerproxy.util;

import eu.openanalytics.containerproxy.event.ConfigUpdateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import java.security.NoSuchAlgorithmException;

@Configuration
public class ConfigUpdateListener{
    protected final Logger log = LogManager.getLogger(getClass());

    @Autowired
    private ConfigFileHelper configFileHelper;

    @Autowired
    private ContextRefresher contextRefresher;

    @EventListener
    public void onUpdate(ConfigUpdateEvent event) throws NoSuchAlgorithmException {
        String hash = configFileHelper.getConfigHash();
        if (hash.equals("unknown")){
            log.info("No active application.yml set");
        }
        else if (hash.equals("illegal")){
            log.error("application.yml syntax error");
        }else{
            log.info("Config changed, new hash = " + hash);
            new Thread(()-> contextRefresher.refresh()).start();
        }
    }

    @EventListener
    public void onOriginalEventPublished(EnvironmentChangeEvent event){
        log.debug("Spring cloud env changed event published, " + event);
    }
}
