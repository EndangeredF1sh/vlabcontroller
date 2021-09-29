/**
 * ContainerProxy
 *
 * Copyright (C) 2016-2021 Open Analytics
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
package eu.openanalytics.containerproxy.api;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.service.UserService;
import eu.openanalytics.containerproxy.util.ProxyMappingManager;
import eu.openanalytics.containerproxy.util.SessionHelper;

@Controller
public class ProxyRouteController extends BaseController {

	@Inject
	private UserService userService;
	
	@Inject
	private ProxyService proxyService;
	
	@Inject
	private ProxyMappingManager mappingManager;
	
	@Inject
	private Environment environment;

	@RequestMapping(value="/api/route/**")
	public void route(HttpServletRequest request, HttpServletResponse response) {
		try {
			String baseURL = SessionHelper.getContextPath(environment, true) + "api/route/";
			String mapping = request.getRequestURI().substring(baseURL.length()).replaceAll("/{2,}", "/");
			String proxyId = mappingManager.getProxyId(mapping);
			String prefix = proxyId;
			if (proxyId != null) {
				boolean isAdmin = userService.isAdmin();
				Proxy proxy = proxyService.findProxy(p -> proxyId.equals(p.getId()), true);
				String[] path = mapping.split("/");
				String mappingType = path.length > 1 ? path[1] : "";
				int targetPort = -1;
				boolean hasAccess = userService.isOwner(proxy);
				if (("/" + mappingType).equals(mappingManager.getProxyPortMappingsEndpoint())) {
					String portString = path[2];
					if (portString != null){
						int port = Integer.parseInt(portString);
						if (port < 0 || port > 65535) {
							response.sendError(404, "Invalid port");
						}else{
							prefix = prefix + "/" + mappingType + "/" + portString;
							targetPort = port;
						}
					}
				}
				if (hasAccess || isAdmin) {
					String subPath = StringUtils.substringAfter(mapping, prefix);
					if (subPath.trim().isEmpty()){
						response.sendRedirect(request.getRequestURI() + "/");
						return;
					}
					if (targetPort >= 0){
						mappingManager.dispatchAsync(proxy, subPath, targetPort, request, response);
					} else {
						mappingManager.dispatchAsync(mapping, request, response);
					}
				} else {
					response.sendError(403);
				}
			} else {
				response.sendError(404, "Proxy is not existed");
			}
		} catch (Exception e) {
			throw new RuntimeException("Error routing proxy request", e);
		}
	}
}
