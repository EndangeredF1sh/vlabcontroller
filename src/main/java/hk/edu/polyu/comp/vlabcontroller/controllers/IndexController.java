package hk.edu.polyu.comp.vlabcontroller.controllers;

import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class IndexController extends BaseController {
    @RequestMapping("/")
    private Object index(ModelMap map, HttpServletRequest request) {
        var landingPage = proxyProperties.getLandingPage();
        if (!landingPage.equals("/")) return new RedirectView(landingPage);
        prepareMap(map, request);
        var apps = proxyService.getProxySpecs(null, false);
        var appLogos = apps.stream()
            .filter(x -> x.getLogoURL() != null)
            .collect(Collectors.toMap(x -> x, x -> resolveImageURI(x.getLogoURL())));
        map.putAll(Map.ofEntries(
            Map.entry("apps", apps.toArray(ProxySpec[]::new)),
            Map.entry("appLogos", appLogos),
            Map.entry("displayAppLogos", !appLogos.isEmpty())
        ));
        return "index";
    }
}
