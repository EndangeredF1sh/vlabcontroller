package hk.edu.polyu.comp.vlabcontroller.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ControlPanelController extends BaseController {
    @RequestMapping("/controlpanel")
    private String panel(ModelMap map, HttpServletRequest request) {
        prepareMap(map, request);
        var username = getUserName(request);
        var proxies = proxyService.getProxies(p -> p.getUserId().equals(username), false);

        var proxyUptimes = AdminController.getUptimes(proxies);

        int containerLimit = proxyProperties.getContainerQuantityLimit();
        map.put("withFileBrowser", proxyService.findProxy(p -> p.getSpec().getId().equals("filebrowser"), false) != null);
        map.put("containerLimit", containerLimit);
        map.put("proxies", proxies);
        map.put("proxyUptimes", proxyUptimes);

        return "panel";
    }
}
