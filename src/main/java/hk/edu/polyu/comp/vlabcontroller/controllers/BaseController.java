package hk.edu.polyu.comp.vlabcontroller.controllers;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import hk.edu.polyu.comp.vlabcontroller.util.SessionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ModelMap;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Principal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseController {

    private static final Logger logger = LogManager.getLogger(BaseController.class);
    private static final Pattern appPattern = Pattern.compile(".*?/app[^/]*/([^/]*)/?.*");
    private static final Pattern portPattern = Pattern.compile(".*/app[^/]*/[^/]*/port/([0-9]{1,5}).*");
    private static final Map<String, String> imageCache = new HashMap<>();

    final ProxyService proxyService;
    final UserService userService;
    final Environment environment;
    final IAuthenticationBackend authenticationBackend;

    @Lazy
    protected BaseController(ProxyService proxyService, UserService userService, Environment environment, IAuthenticationBackend authenticationBackend) {
        this.proxyService = proxyService;
        this.userService = userService;
        this.environment = environment;
        this.authenticationBackend = authenticationBackend;
    }

    protected String getUserName(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return (principal == null) ? request.getSession().getId() : principal.getName();
    }

    protected String getAppPort(HttpServletRequest request) {
        return getAppPort(request.getRequestURI());
    }

    protected String getAppPort(String uri) {
        Matcher matcher = portPattern.matcher(uri);
        return matcher.matches() ? matcher.group(1) : null;
    }

    protected String getAppName(HttpServletRequest request) {
        return getAppName(request.getRequestURI());
    }

    protected String getAppName(String uri) {
        Matcher matcher = appPattern.matcher(uri);
        return matcher.matches() ? matcher.group(1) : null;
    }

    protected String getAppTitle(HttpServletRequest request) {
        String appName = getAppName(request);
        if (appName == null || appName.isEmpty()) return "";
        ProxySpec spec = proxyService.getProxySpec(appName);
        if (spec == null || spec.getDisplayName() == null || spec.getDisplayName().isEmpty()) return appName;
        else return spec.getDisplayName();
    }

    protected String getContextPath() {
        return SessionHelper.getContextPath(environment, true);
    }

    protected Proxy findUserProxy(HttpServletRequest request) {
        String appName = getAppName(request);
        if (appName == null) return null;
        return proxyService.findProxy(p -> appName.equals(p.getSpec().getId()) && userService.isOwner(p), false);
    }

    protected String getProxyEndpoint(Proxy proxy) {
        if (proxy == null || proxy.getTargets().isEmpty()) return null;
        return proxy.getTargets().keySet().iterator().next();
    }

    protected void prepareMap(ModelMap map, HttpServletRequest request) {
        map.put("title", environment.getProperty("proxy.title", "VLabController"));
        map.put("logo", resolveImageURI(environment.getProperty("proxy.logo-url")));
        map.put("instance", environment.getProperty("proxy.identifier-value", "default-identifier"));
        map.put("enableSubDomainMode", !environment.getProperty("proxy.domain", "").isEmpty());

        String hideNavBarParam = request.getParameter("sp_hide_navbar");
        if (Objects.equals(hideNavBarParam, "true")) {
            map.put("showNavbar", false);
        } else {
            map.put("showNavbar", !Boolean.parseBoolean(environment.getProperty("proxy.hide-navbar")));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
        map.put("isLoggedIn", isLoggedIn);
        map.put("isAdmin", userService.isAdmin(authentication));
        map.put("isSupportEnabled", isLoggedIn && getSupportAddress() != null);
        map.put("logoutUrl", authenticationBackend.getLogoutURL());
    }

    protected String getSupportAddress() {
        return environment.getProperty("proxy.support.mail-to-address");
    }

    protected String resolveImageURI(String resourceURI) {
        if (resourceURI == null || resourceURI.isEmpty()) return resourceURI;
        if (imageCache.containsKey(resourceURI)) return imageCache.get(resourceURI);

        String resolvedValue = resourceURI;
        if (resourceURI.toLowerCase().startsWith("file://")) {
            String mimetype = URLConnection.guessContentTypeFromName(resourceURI);
            if (mimetype == null) {
                logger.warn("Cannot determine mimetype for resource: " + resourceURI);
            } else {
                try (InputStream input = new URL(resourceURI).openConnection().getInputStream()) {
                    byte[] data = StreamUtils.copyToByteArray(input);
                    String encoded = Base64.getEncoder().encodeToString(data);
                    resolvedValue = String.format("data:%s;base64,%s", mimetype, encoded);
                } catch (IOException e) {
                    logger.warn("Failed to convert file URI to data URI: " + resourceURI, e);
                }
            }
        }
        imageCache.put(resourceURI, resolvedValue);
        return resolvedValue;
    }
}
