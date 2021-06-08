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

import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.util.RedisSessionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ConditionalOnProperty(prefix = "spring.session", name = "store-type", havingValue = "redis")
public class UserController {
    @Autowired
    private RedisSessionHelper sessionHelper;

    @Autowired
    private ProxyService proxyService;

    @RequestMapping(value = "/api/user/{userid}", method = RequestMethod.DELETE)
    public ResponseEntity<String> cleanup(@PathVariable String userid){
        List<Proxy> proxyList = proxyService.getProxies(p -> p.getUserId().equals(userid), false);
        sessionHelper.logoutByUsername(userid);
        if (proxyList.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        for (Proxy proxy: proxyList){
            proxyService.stopProxy(proxy, true, false);
        }
        return new ResponseEntity<>("cleaned up " + userid, HttpStatus.OK);
    }
}
