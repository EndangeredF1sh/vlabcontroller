/**
 * ShinyProxy
 * <p>
 * Copyright (C) 2016-2021 Open Analytics
 * <p>
 * ===========================================================================
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 * <p>
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
package eu.openanalytics.shinyproxy.controllers;

import eu.openanalytics.containerproxy.auth.IAuthenticationBackend;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.service.UserService;
import eu.openanalytics.containerproxy.util.SessionHelper;
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
    map.put("title", environment.getProperty("proxy.title", "ShinyProxy"));
    map.put("logo", resolveImageURI(environment.getProperty("proxy.logo-url")));
    
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
