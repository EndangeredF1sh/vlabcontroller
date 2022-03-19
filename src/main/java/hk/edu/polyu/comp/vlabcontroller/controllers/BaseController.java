package hk.edu.polyu.comp.vlabcontroller.controllers;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.config.ServerProperties;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import hk.edu.polyu.comp.vlabcontroller.util.SessionHelper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ModelMap;
import org.springframework.util.StreamUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@RefreshScope
@Slf4j
public abstract class BaseController {
    private static final Pattern appPattern = Pattern.compile(".*?/app[^/]*/([^/]*)/?.*");
    private static final Pattern portPattern = Pattern.compile(".*/app[^/]*/[^/]*/port/([0-9]{1,5}).*");
    private static final Map<String, String> imageCache = new HashMap<>();
    
    @Setter(onMethod_ = {@Inject}) protected ProxyService proxyService;
    @Setter(onMethod_ = {@Inject}) protected UserService userService;
    @Setter(onMethod_ = {@Inject}) protected ProxyProperties proxyProperties;
    @Setter(onMethod_ = {@Inject}) protected ServerProperties serverProperties;
    @Setter(onMethod_ = {@Inject}) protected IAuthenticationBackend authenticationBackend;

    protected String getUserName(HttpServletRequest request) {
        var principal = request.getUserPrincipal();
        return (principal == null) ? request.getSession().getId() : principal.getName();
    }

    protected String getAppPort(HttpServletRequest request) {
        return getAppPort(request.getRequestURI());
    }

    protected String getAppPort(String uri) {
        var matcher = portPattern.matcher(uri);
        return matcher.matches() ? matcher.group(1) : null;
    }

    protected String getAppName(HttpServletRequest request) {
        return getAppName(request.getRequestURI());
    }

    protected String getAppName(String uri) {
        var matcher = appPattern.matcher(uri);
        return matcher.matches() ? matcher.group(1) : null;
    }

    protected String getAppTitle(HttpServletRequest request) {
        var appName = getAppName(request);
        if (appName == null || appName.isEmpty()) return "";
        var spec = proxyService.getProxySpec(appName);
        if (spec == null || spec.getDisplayName() == null || spec.getDisplayName().isEmpty()) return appName;
        else return spec.getDisplayName();
    }

    protected String getContextPath() {
        return SessionHelper.getContextPath(serverProperties, true);
    }

    protected Proxy findUserProxy(HttpServletRequest request) {
        var appName = getAppName(request);
        if (appName == null) return null;
        return proxyService.findProxy(p -> appName.equals(p.getSpec().getId()) && userService.isOwner(p), false);
    }

    protected String getProxyEndpoint(Proxy proxy) {
        if (proxy == null || proxy.getTargets().isEmpty()) return null;
        return proxy.getTargets().keySet().iterator().next();
    }

    protected void prepareMap(ModelMap map, HttpServletRequest request) {
        map.put("title", proxyProperties.getTitle());
        map.put("logo", proxyProperties.getLogoUrl());
        map.put("instance", proxyProperties.getIdentifierValue());
        map.put("enableSubDomainMode", !proxyProperties.getDomain().isEmpty());
        var keycloak = proxyProperties.getKeycloak();
        if (proxyProperties.getAuthentication().equals("keycloak") && keycloak != null) {
            var authURL = keycloak.getAuthServerUrl();
            var realm = keycloak.getRealm();
            String accountManagementUrl = null;
            try {
                var uriBuilder = new URIBuilder(authURL);
                var pathSegments = uriBuilder.getPathSegments();
                pathSegments.removeIf(String::isBlank);
                pathSegments.add("realms");
                pathSegments.add(realm);
                pathSegments.add("account");
                uriBuilder.setPathSegments(pathSegments);
                accountManagementUrl = uriBuilder.build().toString();
            } catch (URISyntaxException e) {
                log.error("Keycloak URL syntax error");
            }
            map.put("accountManagementUrl", accountManagementUrl);
        }

        var hideNavBarParam = request.getParameter("sp_hide_navbar");
        if (Objects.equals(hideNavBarParam, "true")) {
            map.put("showNavbar", false);
        } else {
            map.put("showNavbar", !proxyProperties.isHideNavbar());
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var isLoggedIn = authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated();
        map.put("isLoggedIn", isLoggedIn);
        map.put("isAdmin", userService.isAdmin(authentication));
        map.put("isSupportEnabled", isLoggedIn && getSupportAddress() != null);
        map.put("logoutUrl", authenticationBackend.getLogoutURL());
    }

    protected String getSupportAddress() {
        return proxyProperties.getSupportMailToAddress();
    }

    protected String resolveImageURI(String resourceURI) {
        if (resourceURI == null || resourceURI.isEmpty()) return resourceURI;
        if (imageCache.containsKey(resourceURI)) return imageCache.get(resourceURI);

        var resolvedValue = resourceURI;
        if (resourceURI.toLowerCase().startsWith("file://")) {
            var mimetype = URLConnection.guessContentTypeFromName(resourceURI);
            if (mimetype == null) {
                log.warn("Cannot determine mimetype for resource: " + resourceURI);
            } else {
                try (var input = new URL(resourceURI).openConnection().getInputStream()) {
                    var data = StreamUtils.copyToByteArray(input);
                    var encoded = Base64.getEncoder().encodeToString(data);
                    resolvedValue = String.format("data:%s;base64,%s", mimetype, encoded);
                } catch (IOException e) {
                    log.warn("Failed to convert file URI to data URI: " + resourceURI, e);
                }
            }
        }
        imageCache.put(resourceURI, resolvedValue);
        return resolvedValue;
    }
}
