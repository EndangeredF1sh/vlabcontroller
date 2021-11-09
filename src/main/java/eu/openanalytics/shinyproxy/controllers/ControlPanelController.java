package eu.openanalytics.shinyproxy.controllers;

import eu.openanalytics.containerproxy.auth.IAuthenticationBackend;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
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
public class ControlPanelController extends BaseController {
    protected ControlPanelController(ProxyService proxyService, UserService userService, Environment environment, @Lazy IAuthenticationBackend authenticationBackend) {
        super(proxyService, userService, environment, authenticationBackend);
    }

    @RequestMapping("/controlpanel")
    private String panel(ModelMap map, HttpServletRequest request) {
        prepareMap(map, request);
        String username = getUserName(request);
        List<Proxy> proxies = proxyService.getProxies(p -> p.getUserId().equals(username), false);

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

        int containerLimit = environment.getProperty("proxy.container-quantity-limit", Integer.class, 2);
        map.put("withFileBrowser", proxyService.findProxy(p -> p.getSpec().getId().equals("filebrowser"), false) != null);
        map.put("containerLimit", containerLimit);
        map.put("proxies", proxies);
        map.put("proxyUptimes", proxyUptimes);
        map.put("enableSubDomainMode", !environment.getProperty("proxy.domain", "").isEmpty());

        return "panel";
    }
}
