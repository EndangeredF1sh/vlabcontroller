package hk.edu.polyu.comp.vlabcontroller.util;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.PortMappingMetadata;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyMappingMetadata;
import hk.edu.polyu.comp.vlabcontroller.service.HeartbeatService;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;
import io.undertow.util.PathMatcher;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * This component keeps track of which proxy mappings (i.e. URL endpoints) are currently registered,
 * and tells Undertow where they should proxy to.
 */
@Log4j2
@Component
public class ProxyMappingManager {

    private static final String PROXY_INTERNAL_ENDPOINT = "/proxy_endpoint";
    private static final String PROXY_PORT_MAPPINGS_ENDPOINT = "/port_mappings";
    private static final AttachmentKey<ProxyMappingManager> ATTACHMENT_KEY_DISPATCHER = AttachmentKey.create(ProxyMappingManager.class);
    private final Map<String, ProxyMappingMetadata> proxyMappings = new HashMap<>(); // proxyId -> metadata
    private PathHandler pathHandler;
    private final HeartbeatService heartbeatService;

    public ProxyMappingManager(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    public synchronized HttpHandler createHttpHandler(HttpHandler defaultHandler) {
        if (pathHandler == null) {
            pathHandler = new ProxyPathHandler(defaultHandler);
        }
        return pathHandler;
    }

    @SuppressWarnings("deprecation")
    public synchronized void addMapping(String proxyId, String mapping, URI target) {
        if (pathHandler == null)
            throw new IllegalStateException("Cannot change mappings: web server is not yet running.");
        if (proxyMappings.containsKey(proxyId)) {
            if (proxyMappings.get(proxyId).containsExactMappingPath(mapping)) return;
        }
        ProxyMappingMetadata proxyMappingMetadata = proxyMappings.computeIfAbsent(proxyId, value -> new ProxyMappingMetadata());

        LoadBalancingProxyClient proxyClient = new LoadBalancingProxyClient() {
            @Override
            public void getConnection(ProxyTarget target, HttpServerExchange exchange, ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {
                try {
                    exchange.addResponseCommitListener(ex -> heartbeatService.attachHeartbeatChecker(ex, proxyId));
                } catch (Exception e) {
                    log.error(e);
                }
                super.getConnection(target, exchange, callback, timeout, timeUnit);
            }
        };
        proxyClient.setMaxQueueSize(100);
        proxyClient.addHost(target);

        String path = PROXY_INTERNAL_ENDPOINT + "/" + mapping;
        pathHandler.addPrefixPath(path, new ProxyHandler(proxyClient, ResponseCodeHandler.HANDLE_404));

        proxyMappingMetadata.setDefaultTarget(target);
        proxyMappingMetadata.getPortMappingMetadataList().add(new PortMappingMetadata(mapping, target, proxyClient));
        log.debug("mapping {} was added, current mappings: {}", mapping, proxyMappings);
    }

    public synchronized void removeProxyMapping(String proxyId) {
        if (pathHandler == null)
            throw new IllegalStateException("Cannot change mappings: web server is not yet running.");
        if (proxyMappings.containsKey(proxyId)) {
            ProxyMappingMetadata metadata = proxyMappings.get(proxyId);
            metadata.getPortMappingMetadataList().forEach(e -> {
                e.getLoadBalancingProxyClient().closeCurrentConnections();
                e.getLoadBalancingProxyClient().removeHost(e.getTarget());
                pathHandler.removePrefixPath(PROXY_INTERNAL_ENDPOINT + "/"  + e.getPortMapping());
            });
            proxyMappings.remove(proxyId);
        }
        log.debug("removed proxy {}, current mappings: {}", proxyId, proxyMappings);
    }

    public String getProxyId(String mapping) {
        for (Entry<String, ProxyMappingMetadata> e : proxyMappings.entrySet()) {
            ProxyMappingMetadata metadata = e.getValue();
            if (metadata.containsMappingPathPrefix(mapping)) return e.getKey();
        }
        return null;
    }

    public String getProxyPortMappingsEndpoint() {
        return PROXY_PORT_MAPPINGS_ENDPOINT;
    }

    /**
     * Dispatch a request to a target proxy mapping.
     * <p>
     * This approach should be used to dispatch requests from a Spring-secured servlet context
     * to an unsecured Undertow handler.
     * <p>
     * Note that clients can never access a proxy handler directly (for security reasons).
     * Dispatching is the only allowed method to access proxy handlers.
     *
     * @param mapping  The target mapping to dispatch to.
     * @param request  The request to dispatch.
     * @param response The response corresponding to the request.
     * @throws IOException      If the dispatch fails for an I/O reason.
     * @throws ServletException If the dispatch fails for any other reason.
     */
    public void dispatchAsync(String mapping, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpServerExchange exchange = ServletRequestContext.current().getExchange();
        exchange.putAttachment(ATTACHMENT_KEY_DISPATCHER, this);

        String queryString = request.getQueryString();
        queryString = (queryString == null) ? "" : "?" + queryString;
        String targetPath = PROXY_INTERNAL_ENDPOINT + "/" + mapping + queryString;

        request.startAsync();
        request.getRequestDispatcher(targetPath).forward(request, response);
    }

    /**
     * Dispatch a request to a port-mapping target (customized port).
     * Create target before dispatching a request.
     * e.g: create port-mapping target
     * /proxy_endpoint/6480d0f4-d5c1-4bfd-9d73-d9c92f4f1e42/port_mappings/8080/path?query=value -> 10.42.23.100:8080/path?query=value
     * <p>
     * Note that clients should create port-mappings when they are using docker backends.
     * Dispatching is the only allowed method to access proxy handlers.
     *
     * @param proxy    The target's proxy.
     * @param mapping  The target mapping to dispatch to.
     * @param port     The corresponding port for new target.
     * @param request  The request to dispatch.
     * @param response The response corresponding to the request.
     * @throws IOException        If the dispatch fails for an I/O reason.
     * @throws ServletException   If the dispatch fails for any other reason.
     * @throws URISyntaxException If URI syntax is not allowed.
     */
    public void dispatchAsync(Proxy proxy, String mapping, int port, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, URISyntaxException {
        HttpServerExchange exchange = ServletRequestContext.current().getExchange();
        exchange.putAttachment(ATTACHMENT_KEY_DISPATCHER, this);

        String proxyId = proxy.getId();
        URI defaultTarget = proxyMappings.get(proxyId).getDefaultTarget();
        String port_mapping = proxyId + PROXY_PORT_MAPPINGS_ENDPOINT + "/" + port;
        URI newTarget = new URI(defaultTarget.getScheme() + "://" + defaultTarget.getHost() + ":" + port);
        int[] failedResponseCode = new int[1];
        boolean targetConnected = Retrying.retry(i -> {
            try {
                String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
                log.debug("request protocol: {}, scheme: {}, headers: {}", request.getProtocol(), request.getScheme(), Collections.list(request.getHeaderNames()));

                // Handle websocket case
                if (request.getHeaders("Upgrade").hasMoreElements()) {
                    return true;
                }
                URL testURL = new URL(newTarget + mapping + query);
                log.debug("Testing url of {}", testURL);
                HttpURLConnection connection = (HttpURLConnection) testURL.openConnection();
                connection.setConnectTimeout(5000);
                connection.setInstanceFollowRedirects(false);
                int responseCode = connection.getResponseCode();
                log.debug("received connection from {}, status code: {}", testURL, responseCode);
                if (responseCode < 500) {
                    log.debug("successfully connected to target {}", testURL);
                }else{
                    failedResponseCode[0] = responseCode;
                }
                return true;
            }catch (IOException ioe) {
                failedResponseCode[0] = 404;
                log.debug("Trying to connect target URL ({}/{})", i, 5);
            } catch (Exception e) {
                failedResponseCode[0] = 500;
                log.debug(e);
                log.debug("Trying to connect target URL ({}/{})", i, 5);
            }
            return false;
        }, 5, 2000, true);

        if (!targetConnected) {
            response.sendError(failedResponseCode[0]);
            return;
        }
        addMapping(proxyId, port_mapping, newTarget);
        proxy.getTargets().put(port_mapping, newTarget);

        String queryString = request.getQueryString();
        queryString = (queryString == null) ? "" : "?" + queryString;
        String targetPath = PROXY_INTERNAL_ENDPOINT + "/" + port_mapping + mapping + queryString;
        request.startAsync();
        request.getRequestDispatcher(targetPath).forward(request, response);
    }

    private static class ProxyPathHandler extends PathHandler {

        public ProxyPathHandler(HttpHandler defaultHandler) {
            super(defaultHandler);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            Field field = PathHandler.class.getDeclaredField("pathMatcher");
            field.setAccessible(true);
            PathMatcher<HttpHandler> pathMatcher = (PathMatcher<HttpHandler>) field.get(this);
            PathMatcher.PathMatch<HttpHandler> match = pathMatcher.match(exchange.getRelativePath());

            // Note: this handler may never be accessed directly (because it bypasses Spring security).
            // Only allowed if the request was dispatched via this class.
            if (match.getValue() instanceof ProxyHandler && exchange.getAttachment(ATTACHMENT_KEY_DISPATCHER) == null) {
                exchange.setStatusCode(403);
                exchange.getResponseChannel().write(ByteBuffer.wrap("Not authorized to access this proxy".getBytes()));
            } else {
                super.handleRequest(exchange);
            }
        }
    }
}
