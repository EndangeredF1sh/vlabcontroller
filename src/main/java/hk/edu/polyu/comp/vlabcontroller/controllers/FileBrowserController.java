package hk.edu.polyu.comp.vlabcontroller.controllers;

import hk.edu.polyu.comp.vlabcontroller.VLabControllerException;
import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyStatus;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import hk.edu.polyu.comp.vlabcontroller.spec.FileBrowserProperties;
import hk.edu.polyu.comp.vlabcontroller.util.Retrying;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
public class FileBrowserController extends BaseController {

    private final Logger log = LogManager.getLogger(FileBrowserController.class);

    private final FileBrowserProperties fileBrowserProperties;

    protected FileBrowserController(ProxyService proxyService, UserService userService, Environment environment, @Lazy IAuthenticationBackend authenticationBackend, FileBrowserProperties fileBrowserProperties) {
        super(proxyService, userService, environment, authenticationBackend);
        this.fileBrowserProperties = fileBrowserProperties;
    }


    @RequestMapping(value = "/filebrowser/**")
    public String fileBrowser(ModelMap map, HttpServletRequest request) {
        prepareMap(map, request);
        String id = "filebrowser";
        Proxy proxy = proxyService.findProxy(p -> p.getSpec().getId().equals(id) && userService.isOwner(p), false);
        awaitReady(proxy);
        map.put("appTitle", "File Browser");
        map.put("container", (proxy == null) ? "" : buildContainerPath(request));
        return "filebrowser";
    }

    @RequestMapping(value = "/filebrowser/**", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> startFileBrowser(HttpServletRequest request) {
        String id = "filebrowser";
        Proxy proxy = proxyService.findProxy(p -> p.getSpec().getId().equals(id) && userService.isOwner(p), false);
        if (proxy == null) {
            if (fileBrowserProperties != null) {
                ProxySpec spec = fileBrowserSpecTranslate(fileBrowserProperties);
                ProxySpec resolvedSpec = proxyService.resolveProxySpec(spec, null, null);
                try {
                    proxy = proxyService.startProxy(resolvedSpec, false);
                } catch (VLabControllerException e) {
                    String errorMessage = "Failed to start file browser";
                    log.error(errorMessage);
                    log.debug(e);
                    Map<String, String> response = new HashMap<>();
                    response.put("error_code", "404");
                    response.put("error_message", errorMessage);
                    return response;
                }
            } else {
                String errorMessage = "Missing file browser spec";
                log.error(errorMessage);
                Map<String, String> response = new HashMap<>();
                response.put("error_code", "404");
                response.put("error_message", errorMessage);
                return response;
            }
        }
        awaitReady(proxy);
        String containerPath = buildContainerPath(request);
        Map<String, String> response = new HashMap<>();
        response.put("containerPath", containerPath);
        return response;
    }

    private ProxySpec fileBrowserSpecTranslate(FileBrowserProperties fbp) {
        ProxySpec spec = new ProxySpec();
        fbp.copy(spec);
        spec.setId("filebrowser");
        spec.setDisplayName("File Browser");
        spec.setTemplateName("File Browser");
        return spec;
    }

    private boolean awaitReady(Proxy proxy) {
        return awaitReadyHelper(proxy, environment.getProperty("proxy.container-wait-time", "20000"));
    }

    static boolean awaitReadyHelper(Proxy proxy, String property) {
        if (proxy == null) return false;
        if (proxy.getStatus() == ProxyStatus.Up) return true;
        if (proxy.getStatus() == ProxyStatus.Stopping || proxy.getStatus() == ProxyStatus.Stopped) return false;

        int totalWaitMs = Integer.parseInt(property);
        int waitMs = Math.min(500, totalWaitMs);
        int maxTries = totalWaitMs / waitMs;
        Retrying.retry(i -> proxy.getStatus() != ProxyStatus.Starting, maxTries, waitMs);

        return (proxy.getStatus() == ProxyStatus.Up);
    }

    private String buildContainerPath(HttpServletRequest request) {
        String queryString = ServletUriComponentsBuilder.fromRequest(request).replaceQueryParam("sp_hide_navbar").build().getQuery();
        queryString = (queryString == null) ? "" : "?" + queryString;
        var containerPathPattern = Pattern.compile(".*?/filebrowser[/]*(.*)");
        var matcher = containerPathPattern.matcher(request.getRequestURI());
        String containerPath = matcher.find() ? matcher.group(1) + queryString : queryString;
        return getContextPath() + "app_direct/filebrowser" + "/" + containerPath;
    }
}