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

package eu.openanalytics.containerproxy.service;

import eu.openanalytics.containerproxy.event.ConfigUpdateEvent;
import eu.openanalytics.containerproxy.util.ConfigFileHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.NoSuchAlgorithmException;

@RefreshScope
@Service
public class FileUpdateService extends Thread{
    protected final Logger log = LogManager.getLogger(getClass());

    @Autowired
    private ConfigFileHelper configFileHelper;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Value("${proxy.config.interval:5000}")
    private int interval;

    @Value("${proxy.config.auto-update:true}")
    private boolean configAutoUpdate;

    @PostConstruct
    public void start(){
        if (configAutoUpdate) {
            log.info(String.format("Starting configuration auto detection, interval: %dms", interval));
            super.start();
        }
    }

    @Override
    public void run(){
        try{
            String before = configFileHelper.getConfigHash();
            while(true){
                String after = configFileHelper.getConfigHash();
                if (!before.equals(after)){
                    publisher.publishEvent(new ConfigUpdateEvent(this));
                }
                before = after;
                Thread.sleep(interval);
            }
        }catch (NoSuchAlgorithmException | InterruptedException e){
            e.printStackTrace();
        }
    }
}
