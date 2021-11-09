package eu.openanalytics.shinyproxy.controllers;

import eu.openanalytics.containerproxy.auth.IAuthenticationBackend;
import eu.openanalytics.containerproxy.model.runtime.HeartbeatStatus;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.service.HeartbeatService;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController extends BaseController {

    private final HeartbeatService heartbeatService;

    protected AdminController(ProxyService proxyService, UserService userService, Environment environment, IAuthenticationBackend authenticationBackend, @Lazy HeartbeatService heartbeatService) {
        super(proxyService, userService, environment, authenticationBackend);
        this.heartbeatService = heartbeatService;
    }


    @RequestMapping("/admin")
    private String admin(ModelMap map, HttpServletRequest request) {
        prepareMap(map, request);

        List<Proxy> proxies = proxyService.getProxies(null, false);
        Map<String, String> proxyUptimes = new HashMap<>();
        for (Proxy proxy : proxies) {
            long uptimeSec = 0;
            // if the proxy hasn't started up yet, the uptime should be zero
            if (proxy.getStartupTimestamp() > 0) {
                uptimeSec = (System.currentTimeMillis() - proxy.getStartupTimestamp()) / 1000;
            }
            String uptime = String.format("%d:%02d:%02d", uptimeSec / 3600, (uptimeSec % 3600) / 60, uptimeSec % 60);
            proxyUptimes.put(proxy.getId(), uptime);
        }

        Map<String, Long> proxyHeartbeats = heartbeatService.getProxyHeartbeats();
        Map<String, HeartbeatStatus> websocketHeartbeats = heartbeatService.getWebsocketHeartbeats();
        long heartbeatRate = Long.parseLong(environment.getProperty("proxy.heartbeat-rate", "60000"));

        Map<String, Long> lastActive = new HashMap<>();
        proxyHeartbeats.forEach((k, v) -> {
            long httpRequestActiveTimestamp = v;
            HeartbeatStatus hbs = websocketHeartbeats.get(k);
            if (hbs != null) {
                long websocketActiveTimestamp = hbs.getLastRecordTimestamp() - hbs.getTerminateCounter() * heartbeatRate;
                lastActive.put(k, Math.max(websocketActiveTimestamp, httpRequestActiveTimestamp));
            } else {
                lastActive.put(k, httpRequestActiveTimestamp);
            }
        });

        map.put("proxies", proxies);
        map.put("proxyUptimes", proxyUptimes);
        map.put("lastActive", lastActive);
        map.put("enableSubDomainMode", !environment.getProperty("proxy.domain", "").isEmpty());

        return "admin";
    }
}
