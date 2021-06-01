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

package eu.openanalytics.containerproxy.api;

import eu.openanalytics.containerproxy.event.ConfigUpdateEvent;
import eu.openanalytics.containerproxy.util.ConfigFileHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;

@ConditionalOnExpression("${proxy.config.enable-refresh-api:false}")
@RestController
public class ConfigController {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private ConfigFileHelper configFileHelper;

    @RequestMapping(value = "/config/refresh")
    public ResponseEntity<String> refresh() throws NoSuchAlgorithmException {
        String hash = configFileHelper.getConfigHash();
        publisher.publishEvent(new ConfigUpdateEvent(this));
        return new ResponseEntity<>(hash, HttpStatus.OK);
    }
}
