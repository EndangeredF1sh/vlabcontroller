package hk.edu.polyu.comp.vlabcontroller.api;

import hk.edu.polyu.comp.vlabcontroller.config.ServerProperties;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import hk.edu.polyu.comp.vlabcontroller.util.ProxyMappingManager;
import hk.edu.polyu.comp.vlabcontroller.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequiredArgsConstructor
public class ProxyRouteController extends BaseController {
    private final UserService userService;
    private final ProxyService proxyService;
    private final ProxyMappingManager mappingManager;
    private final ServerProperties serverProperties;

    @RequestMapping(value = "/api/route/**")
    public void route(HttpServletRequest request, HttpServletResponse response) {
        try {
            var baseURL = SessionHelper.getContextPath(serverProperties, true) + "api/route/";
            var mapping = request.getRequestURI().substring(baseURL.length()).replaceAll("/{2,}", "/");
            var proxyId = mappingManager.getProxyId(mapping);
            var prefix = proxyId;
            if (proxyId != null) {
                var isAdmin = userService.isAdmin();
                var proxy = proxyService.findProxy(p -> proxyId.equals(p.getId()), true);
                var path = mapping.split("/");
                var mappingType = path.length > 1 ? path[1] : "";
                var targetPort = -1;
                var hasAccess = userService.isOwner(proxy);
                if (("/" + mappingType).equals(mappingManager.getProxyPortMappingsEndpoint())) {
                    var portString = path[2];
                    if (portString != null) {
                        var port = Integer.parseInt(portString);
                        if (port < 0 || port > 65535) {
                            response.sendError(404, "Invalid port");
                        } else {
                            prefix = prefix + "/" + mappingType + "/" + portString;
                            targetPort = port;
                        }
                    }
                }
                if (hasAccess || isAdmin) {
                    var subPath = StringUtils.substringAfter(mapping, prefix);
                    if (subPath.trim().isEmpty()) {
                        response.sendRedirect(request.getRequestURI() + "/");
                        return;
                    }
                    if (targetPort >= 0) {
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
