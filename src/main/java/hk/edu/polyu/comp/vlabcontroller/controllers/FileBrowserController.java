package hk.edu.polyu.comp.vlabcontroller.controllers;

import hk.edu.polyu.comp.vlabcontroller.VLabControllerException;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyStatus;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.util.DurationUtil;
import hk.edu.polyu.comp.vlabcontroller.util.Retrying;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Controller
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
@RefreshScope
public class FileBrowserController extends BaseController {
    private final ProxyProperties proxyProperties;
    private final Retrying retrying;

    @RequestMapping(value = "/filebrowser/**")
    public String fileBrowser(ModelMap map, HttpServletRequest request) {
        prepareMap(map, request);
        var id = "filebrowser";
        var proxy = proxyService.findProxy(p -> p.getSpec().getId().equals(id) && userService.isOwner(p), false);
        awaitReady(proxy);
        map.put("appTitle", "File Browser");
        map.put("container", (proxy == null) ? "" : buildContainerPath(request));
        return "filebrowser";
    }

    @RequestMapping(value = "/filebrowser/**", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> startFileBrowser(HttpServletRequest request) {
        var id = "filebrowser";
        var proxy = proxyService.findProxy(p -> p.getSpec().getId().equals(id) && userService.isOwner(p), false);
        if (proxy == null) {
            var fileBrowser = proxyProperties.getFileBrowser();
            if (fileBrowser != null) {
                var spec = fileBrowserSpecTranslate(fileBrowser);
                var resolvedSpec = proxyService.resolveProxySpec(spec, null, null);
                try {
                    proxy = proxyService.startProxy(resolvedSpec, false);
                } catch (VLabControllerException e) {
                    var errorMessage = "Failed to start file browser";
                    log.error(errorMessage);
                    log.debug("error details: {}", e);
                    Map<String, String> response = new HashMap<>();
                    response.put("error_code", "404");
                    response.put("error_message", errorMessage);
                    return response;
                }
            } else {
                var errorMessage = "Missing file browser spec";
                log.error(errorMessage);
                Map<String, String> response = new HashMap<>();
                response.put("error_code", "404");
                response.put("error_message", errorMessage);
                return response;
            }
        }
        awaitReady(proxy);
        var containerPath = buildContainerPath(request);
        Map<String, String> response = new HashMap<>();
        response.put("containerPath", containerPath);
        return response;
    }

    private ProxySpec fileBrowserSpecTranslate(ProxySpec fbp) {
        return fbp.copyBuilder().id("filebrowser").displayName("File Browser").build();
    }

    private boolean awaitReady(Proxy proxy) {
        return awaitReadyHelper(proxy, proxyProperties.getContainerWaitTime(), retrying);
    }

    static boolean awaitReadyHelper(Proxy proxy, Duration delay, Retrying retrying) {
        if (proxy == null) return false;
        if (proxy.getStatus() == ProxyStatus.Up) return true;
        if (proxy.getStatus() == ProxyStatus.Stopping || proxy.getStatus() == ProxyStatus.Stopped) return false;

        var waitMs = DurationUtil.atLeast(Duration.ofMillis(500)).apply(delay);
        var maxTries = (int) delay.dividedBy(waitMs);
        retrying.retry(i -> proxy.getStatus() != ProxyStatus.Starting, maxTries, waitMs);

        return (proxy.getStatus() == ProxyStatus.Up);
    }

    private String buildContainerPath(HttpServletRequest request) {
        var queryString = ServletUriComponentsBuilder.fromRequest(request).replaceQueryParam("sp_hide_navbar").build().getQuery();
        queryString = (queryString == null) ? "" : "?" + queryString;
        var containerPathPattern = Pattern.compile(".*?/filebrowser[/]*(.*)");
        var matcher = containerPathPattern.matcher(request.getRequestURI());
        var containerPath = matcher.find() ? matcher.group(1) + queryString : queryString;
        return getContextPath() + "app_direct/filebrowser" + "/" + containerPath;
    }
}