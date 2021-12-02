package eu.openanalytics.shinyproxy.controllers;

import eu.openanalytics.containerproxy.auth.IAuthenticationBackend;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class IndexController extends BaseController {
    protected IndexController(ProxyService proxyService, UserService userService, Environment environment, @Lazy IAuthenticationBackend authenticationBackend) {
        super(proxyService, userService, environment, authenticationBackend);
    }

    @RequestMapping("/")
    private Object index(ModelMap map, HttpServletRequest request) {
        String landingPage = environment.getProperty("proxy.landing-page", "/");
        if (!landingPage.equals("/")) return new RedirectView(landingPage);

        prepareMap(map, request);

        ProxySpec[] apps = proxyService.getProxySpecs(null, false).toArray(new ProxySpec[0]);
        map.put("apps", apps);

        Map<ProxySpec, String> appLogos = new HashMap<>();
        map.put("appLogos", appLogos);

        boolean displayAppLogos = false;
        for (ProxySpec app : apps) {
            if (app.getLogoURL() != null) {
                displayAppLogos = true;
                appLogos.put(app, resolveImageURI(app.getLogoURL()));
            }
        }
        map.put("displayAppLogos", displayAppLogos);
        return "index";
    }
}
