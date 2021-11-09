package eu.openanalytics.shinyproxy.controllers;

import com.google.common.base.Strings;
import eu.openanalytics.containerproxy.ContainerProxyException;
import eu.openanalytics.containerproxy.auth.IAuthenticationBackend;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.service.UserService;
import eu.openanalytics.containerproxy.util.ProxyMappingManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.openanalytics.shinyproxy.controllers.FileBrowserController.awaitReadyHelper;

@Controller
public class AppController extends BaseController {

    private final Logger log = LogManager.getLogger(AppController.class);

    private final ProxyMappingManager mappingManager;

    protected AppController(ProxyService proxyService, UserService userService, Environment environment, @Lazy IAuthenticationBackend authenticationBackend, ProxyMappingManager mappingManager) {
        super(proxyService, userService, environment, authenticationBackend);
        this.mappingManager = mappingManager;
    }


    @RequestMapping(value = "/app/**", method = RequestMethod.GET)
    public String app(ModelMap map, HttpServletRequest request,
                      @ModelAttribute("s") String subDomain,
                      @ModelAttribute("md") String markdownEncodedUrl) {
        prepareMap(map, request);

        Proxy proxy = findUserProxy(request);
        if (proxy == null && !userService.isAdmin()) {
            int containerLimit = environment.getProperty("proxy.container-quantity-limit", Integer.class, 2);
            int proxies = proxyService.getProxies(p -> p.getUserId().equals(userService.getCurrentUserId()) && !p.getSpec().getId().equals("filebrowser"), false).size();
            if (proxies >= containerLimit) {
                return "limit-error";
            }
        }
        awaitReady(proxy);

        map.put("appTitle", getAppTitle(request));
        String baseDomain = environment.getProperty("proxy.domain");
        map.put("baseDomain", baseDomain);
        if (!Strings.isNullOrEmpty(subDomain)) {
            subDomain = new String(Base64.getDecoder().decode(subDomain), StandardCharsets.UTF_8);
            String URL = "https://" + (subDomain.endsWith(".") ? subDomain : subDomain + ".") + baseDomain;
            map.put("subDomainMode", true);
            map.put("iframeURL", URL);
            map.put("container", (proxy == null) ? "" : URL);
        } else {
            map.put("container", (proxy == null) ? "" : buildContainerPath(request));
        }
        map.put("proxyId", (proxy == null) ? "" : proxy.getId());
        map.put("startTime", (proxy == null) ? System.currentTimeMillis() : proxy.getStartupTimestamp());
        map.put("maxAge", Duration.parse(environment.getProperty("proxy.engagement.max-age", "PT4H")).toMillis());
        String markdownURL;
        try {
            markdownURL = new String(Base64.getUrlDecoder().decode(markdownEncodedUrl), StandardCharsets.UTF_8);
            Pattern urlPattern = Pattern.compile("(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
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
            Proxy proxy = getOrStart(request);
            String containerPath = buildContainerPath(request);

            Map<String, String> response = new HashMap<>();
            response.put("containerPath", containerPath);
            response.put("proxyId", proxy.getId());
            return response;
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            log.debug(e);
            Map<String, String> response = new HashMap<>();
            response.put("error_code", "404");
            response.put("error_message", "Unable to find application: " + getAppName(request));
            return response;
        } catch (ContainerProxyException e) {
            log.error(e.getMessage() + ": " + getAppName(request));
            log.debug(e);
            Map<String, String> response = new HashMap<>();
            response.put("error_code", "404");
            response.put("error_message", "Failed to start application: " + getAppName(request));
            return response;
        }
    }

    @RequestMapping(value = "/app_direct/**")
    public void appDirect(HttpServletRequest request, HttpServletResponse response) {
        Proxy proxy = findUserProxy(request);
        awaitReady(proxy);

        String mapping = getProxyEndpoint(proxy);
        String appPort = getAppPort(request);
        String subPath = request.getRequestURI();
        subPath = subPath.substring(subPath.indexOf("/app_direct/") + 12);
        subPath = subPath.substring(getAppName(request).length());
        int port = -1;
        if (appPort != null) {
            port = Integer.parseInt(appPort);
            subPath = subPath.substring(("/port/" + appPort).length());
        }

        if (subPath.trim().isEmpty()) {
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
            response.setStatus(404);
            log.error("Error routing proxy request: {}", request.getRequestURI());
        }
    }

    @RequestMapping(value = "/redirect/*", method = RequestMethod.GET)
    private String subDomainRedirection(ModelMap map, HttpServletRequest request,
                                        RedirectAttributes redirectAttributes,
                                        @ModelAttribute("md") String markdownUrl) {
        try {
            String subDomain = request.getServletPath().substring("/redirect/".length());
            String[] args = subDomain.split("--");
            String appID = args[args.length - 2];
            redirectAttributes.addAttribute("md", markdownUrl);
            redirectAttributes.addAttribute("s", Base64.getEncoder().encodeToString(subDomain.getBytes(StandardCharsets.UTF_8)));
            return "redirect:/app/" + appID;
        } catch (ArrayIndexOutOfBoundsException e) {
            map.put("status", 404);
            return "error";
        }
    }

    private Proxy getOrStart(HttpServletRequest request) {
        Proxy proxy = findUserProxy(request);
        if (proxy == null) {
            String specId = getAppName(request);
            ProxySpec spec = proxyService.getProxySpec(specId);
            if (spec == null) throw new IllegalArgumentException("Unknown proxy spec: " + specId);
            ProxySpec resolvedSpec = proxyService.resolveProxySpec(spec, null, null);
            proxy = proxyService.startProxy(resolvedSpec, false);
        }
        return proxy;
    }

    private boolean awaitReady(Proxy proxy) {
        return awaitReadyHelper(proxy, environment.getProperty("proxy.container-wait-time", "20000"));
    }

    private String buildContainerPath(HttpServletRequest request) {
        String appName = getAppName(request);
        if (appName == null) return "";

        String queryString = ServletUriComponentsBuilder.fromRequest(request).replaceQueryParam("sp_hide_navbar").build().getQuery();

        queryString = (queryString == null) ? "" : "?" + queryString;
        Pattern containerPathPattern = Pattern.compile(".*?/app[^/]*/[^/]*/?(.*)");
        Matcher matcher = containerPathPattern.matcher(request.getRequestURI());
        String containerPath = matcher.find() ? matcher.group(1) + queryString : queryString;
        return getContextPath() + "app_direct/" + appName + "/" + containerPath;
    }
}
