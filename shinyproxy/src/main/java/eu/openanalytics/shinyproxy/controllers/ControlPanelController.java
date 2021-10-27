/**
 * ShinyProxy
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
package eu.openanalytics.shinyproxy.controllers;

import eu.openanalytics.containerproxy.model.runtime.Proxy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ControlPanelController extends BaseController {

    @RequestMapping("/controlpanel")
    private String panel(ModelMap map, HttpServletRequest request){
        prepareMap(map, request);
        String username = getUserName(request);
        List<Proxy> proxies = proxyService.getProxies(p -> p.getUserId().equals(username), false);

        Map<String, String> proxyUptimes = new HashMap<>();
        for (Proxy proxy: proxies) {
            long uptimeSec = 0;
            // if the proxy hasn't started up yet, the uptime should be zero
            if (proxy.getStartupTimestamp() > 0) {
                uptimeSec = (System.currentTimeMillis() - proxy.getStartupTimestamp())/1000;
            }
            String uptime = String.format("%d:%02d:%02d", uptimeSec/3600, (uptimeSec%3600)/60, uptimeSec%60);
            proxyUptimes.put(proxy.getId(), uptime);
        }

        int containerLimit = environment.getProperty("proxy.container-quantity-limit", Integer.class, 2);
        map.put("withFileBrowser", proxyService.findProxy(p -> p.getSpec().getId().equals("filebrowser"), false) != null);
        map.put("containerLimit", containerLimit);
        map.put("proxies", proxies);
        map.put("proxyUptimes", proxyUptimes);
        map.put("enableSubDomainMode", !environment.getProperty("proxy.domain", "").isEmpty());

        return "panel";
    }
}
