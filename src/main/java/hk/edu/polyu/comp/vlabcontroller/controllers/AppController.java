package hk.edu.polyu.comp.vlabcontroller.controllers;

import hk.edu.polyu.comp.vlabcontroller.VLabControllerException;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.spec.EntryPointSpec;
import hk.edu.polyu.comp.vlabcontroller.util.ProxyMappingManager;
import hk.edu.polyu.comp.vlabcontroller.util.Retrying;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static hk.edu.polyu.comp.vlabcontroller.controllers.FileBrowserController.awaitReadyHelper;

@Slf4j
@Controller
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class AppController extends BaseController {
    private final ProxyMappingManager mappingManager;
    private final Retrying retrying;

    @RequestMapping(value = "/app/**", method = RequestMethod.GET)
    public String app(ModelMap map, HttpServletRequest request,
                      @ModelAttribute("s") String innerURI,
                      @ModelAttribute("md") String markdownEncodedUrl) {
        prepareMap(map, request);

        var proxy = findUserProxy(request);
        var hasProxy = proxy == null;
        if (hasProxy && !userService.isAdmin()) {
            int containerLimit = proxyProperties.getContainerQuantityLimit();
            var proxies = proxyService.getProxies(p -> p.getUserId().equals(userService.getCurrentUserId()) && !p.getSpec().getId().equals("filebrowser"), false).size();
            if (proxies >= containerLimit) {
                return "limit-error";
            }
        }
        awaitReady(proxy);

        map.put("appTitle", getAppTitle(request));
        var baseDomain = proxyProperties.getDomain();
        map.put("baseDomain", baseDomain);
        if (innerURI != null && !innerURI.isEmpty()) {
            innerURI = new String(Base64.getDecoder().decode(innerURI), StandardCharsets.UTF_8);
            map.put("subDomainMode", true);
            map.put("iframeURL", innerURI);
            map.put("container", hasProxy ? "" : innerURI);
        } else {
            map.put("container", hasProxy ? "" : buildContainerPath(request));
        }
        map.put("proxyId", hasProxy ? "" : proxy.getId());
        map.put("startTime", hasProxy ? System.currentTimeMillis() : proxy.getStartupTimestamp());
        map.put("maxAge", proxyProperties.getMaxAge().toMillis());
        String markdownURL;
        try {
            markdownURL = new String(Base64.getUrlDecoder().decode(markdownEncodedUrl), StandardCharsets.UTF_8);
            var urlPattern = Pattern.compile("(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
            if (urlPattern.matcher(markdownURL).matches()) {
                markdownURL = URLEncoder.encode(markdownURL, StandardCharsets.UTF_8.toString());
            } else {
                markdownURL = null;
            }
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            markdownURL = null;
        }
        map.put("markdownUrl", markdownURL);
        return "app";
    }

    @RequestMapping(value = "/app/**", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> startApp(HttpServletRequest request) {
        try {
            var proxy = getOrStart(request);
            var containerPath = buildContainerPath(request);

            Map<String, String> response = new HashMap<>();
            response.put("containerPath", containerPath);
            response.put("proxyId", proxy.getId());
            return response;
        } catch (IllegalArgumentException | VLabControllerException e) {
            log.error(e.getMessage() + ": " + getAppName(request));
            log.debug("an error occured: {}", e);
            return Map.ofEntries(
                Map.entry("error_code", "404"),
                Map.entry("error_message",
                    (e instanceof IllegalArgumentException
                        ? "Unable to find application: "
                        : "Failed to start application: ") + getAppName(request))
            );
        }
    }

    @RequestMapping(value = "/app_direct/**")
    public void appDirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var proxy = findUserProxy(request);
        awaitReady(proxy);

        var mapping = getProxyEndpoint(proxy);
        var appPort = getAppPort(request);
        var subPath = request.getRequestURI();
        subPath = subPath.substring(subPath.indexOf("/app_direct/") + 12);
        subPath = subPath.substring(getAppName(request).length());
        var port = -1;
        if (appPort != null) {
            port = Integer.parseInt(appPort);
            subPath = subPath.substring(("/port/" + appPort).length());
        }

        if (subPath.isBlank()) {
            try {
                response.sendRedirect(request.getRequestURI() + "/");
            } catch (Exception e) {
                throw new RuntimeException("Error redirecting proxy request", e);
            }
            return;
        }

        try {
            if (port >= 0) {
                mappingManager.dispatchAsync(proxy, subPath, port, request, response);
            } else {
                mappingManager.dispatchAsync(mapping + subPath, request, response);
            }
        } catch (Exception e) {
            response.sendError(404);
            log.debug("Error routing proxy request: {}", request.getRequestURI());
        }
    }

    @RequestMapping(value = "/redirect/*", method = RequestMethod.GET)
    private String subDomainRedirection(ModelMap map, HttpServletRequest request,
                                        RedirectAttributes redirectAttributes,
                                        @ModelAttribute("md") String markdownUrl) {
        try {
            var servletPath = request.getServletPath().substring("/redirect/".length()).split("/", -1);
            var subDomain = servletPath[0];
            var path = servletPath[1];
            var baseDomain = proxyProperties.getDomain();
            var args = subDomain.split("--");
            var appID = args[args.length - 2];
            var spec = proxyService.getProxySpec(appID);

            var apps = (List<EntryPointSpec>) spec.getSettings().get("entrypoint");

            var entryPointSpec = apps.stream().filter(p -> args[0].equals(Integer.toString(p.getPort()))).collect(Collectors.toList()).get(0);
            var innerURI = new URIBuilder();
            innerURI.setScheme("https");
            innerURI.setHost(subDomain + "." + baseDomain);
            innerURI.setPath(path);
            entryPointSpec.getParameters().forEach(innerURI::setParameter);
            redirectAttributes.addAttribute("md", markdownUrl);
            redirectAttributes.addAttribute("s", Base64.getEncoder().encodeToString(innerURI.build().toString().getBytes(StandardCharsets.UTF_8)));
            return "redirect:/app/" + appID;
        } catch (ArrayIndexOutOfBoundsException | URISyntaxException e) {
            map.put("status", 404);
            return "error";
        }
    }

    private Proxy getOrStart(HttpServletRequest request) {
        var proxy = findUserProxy(request);
        if (proxy == null) {
            var specId = getAppName(request);
            var spec = proxyService.getProxySpec(specId);
            if (spec == null) throw new IllegalArgumentException("Unknown proxy spec: " + specId);
            var resolvedSpec = proxyService.resolveProxySpec(spec, null, null);
            proxy = proxyService.startProxy(resolvedSpec, false);
        }
        return proxy;
    }

    private boolean awaitReady(Proxy proxy) {
        return awaitReadyHelper(proxy, proxyProperties.getContainerWaitTime(), retrying);
    }

    private String buildContainerPath(HttpServletRequest request) {
        var appName = getAppName(request);
        if (appName == null) return "";

        var queryString = ServletUriComponentsBuilder.fromRequest(request).replaceQueryParam("sp_hide_navbar").build().getQuery();

        queryString = (queryString == null) ? "" : "?" + queryString;
        var containerPathPattern = Pattern.compile(".*?/app[^/]*/[^/]*/?(.*)");
        var matcher = containerPathPattern.matcher(request.getRequestURI());
        var containerPath = matcher.find() ? matcher.group(1) + queryString : queryString;
        return getContextPath() + "app_direct/" + appName + "/" + containerPath;
    }
}
