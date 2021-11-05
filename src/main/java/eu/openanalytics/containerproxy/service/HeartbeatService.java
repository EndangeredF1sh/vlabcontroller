package eu.openanalytics.containerproxy.service;

import eu.openanalytics.containerproxy.model.runtime.HeartbeatStatus;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.model.runtime.ProxyStatus;
import eu.openanalytics.containerproxy.spec.EngagementProperties;
import eu.openanalytics.containerproxy.util.ChannelActiveListener;
import eu.openanalytics.containerproxy.util.DelegatingStreamSinkConduit;
import eu.openanalytics.containerproxy.util.DelegatingStreamSourceConduit;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpServerConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.xnio.StreamConnection;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class HeartbeatService {

    private static final String PROP_ENABLED = "proxy.heartbeat-enabled";
    private static final String PROP_RATE = "proxy.heartbeat-rate";
    private static final String PROP_TIMEOUT = "proxy.heartbeat-timeout";

    private static final byte[] WEBSOCKET_PING = {(byte) 0b10001001, (byte) 0b00000000};
    private static final byte WEBSOCKET_PONG = (byte) 0b10001010;

    private final Logger log = LogManager.getLogger(HeartbeatService.class);

    private final Map<String, Long> proxyHeartbeats = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, HeartbeatStatus> websocketHeartbeats = Collections.synchronizedMap(new HashMap<>());
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(3);
    private final ProxyService proxyService;
    private final Environment environment;
    private volatile boolean enabled;
    @Resource
    private EngagementProperties engagementProperties;

    public HeartbeatService(ProxyService proxyService, Environment environment) {
        this.proxyService = proxyService;
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        enabled = Boolean.valueOf(environment.getProperty(PROP_ENABLED, "false"));

        if (environment.getProperty(PROP_ENABLED) == null) {
            enabled = environment.getProperty(PROP_RATE) != null || environment.getProperty(PROP_TIMEOUT) != null;
        }

        Thread cleanupThread = new Thread(new InactiveProxyKiller(), InactiveProxyKiller.class.getSimpleName());
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, HeartbeatStatus> getWebsocketHeartbeats() {
        return websocketHeartbeats;
    }

    public Map<String, Long> getProxyHeartbeats() {
        return proxyHeartbeats;
    }

    public void attachHeartbeatChecker(HttpServerExchange exchange, String proxyId) {
        if (exchange.isUpgrade()) {
            // For websockets, attach a ping-pong listener to the underlying TCP channel.
            HeartbeatConnector connector = new HeartbeatConnector(proxyId);
            // Delay the wrapping, because Undertow will make changes to the channel while the upgrade is being performed.
            HttpServerConnection httpConn = (HttpServerConnection) exchange.getConnection();
            heartbeatExecutor.schedule(() -> connector.wrapChannels(httpConn.getChannel()), 3000, TimeUnit.MILLISECONDS);
        } else {
            // request URI prefix filter
            // exchange.getRequestPath() == /proxy_endpoint/<proxy_uuid>/<real-path-in-container>
            // exchange.getRelativePath() == /<real-path-in-container>
            // e.g access http://<domain-name>/<spring-context-path>/app/app_name/static/js/example.js
            // exchange.getRequestPath() == /proxy_endpoint/<proxy-uuid>/static/js/example.js
            // exchange.getRelativePath() == /static/js/example.js
            for (String path : engagementProperties.getFilterPath()) {
                String relativeRequestPath = exchange.getRelativePath();
                log.debug("Client requests {} to proxy {}", relativeRequestPath, proxyId);
                if (relativeRequestPath.startsWith(path)) {
                    log.debug("Matched prefix {} of proxy {}", path, proxyId);
                    return;
                }
            }
            // For regular HTTP requests, just trigger one heartbeat.
            heartbeatReceived(proxyId);
        }
    }

    private void heartbeatReceived(String proxyId) {
        Proxy proxy = proxyService.getProxy(proxyId);
        if (log.isDebugEnabled()) log.debug("Heartbeat received for proxy " + proxyId);
        if (proxy != null) proxyHeartbeats.put(proxyId, System.currentTimeMillis());
    }

    private long getHeartbeatRate() {
        return Long.parseLong(environment.getProperty(PROP_RATE, "10000"));
    }

    private long getHeartbeatTimeout() {
        return Long.parseLong(environment.getProperty(PROP_TIMEOUT, "60000"));
    }

    private class HeartbeatConnector {

        private final String proxyId;

        public HeartbeatConnector(String proxyId) {
            this.proxyId = proxyId;
        }

        private void wrapChannels(StreamConnection streamConn) {
            if (!streamConn.isOpen()) return;

            ConduitStreamSinkChannel sinkChannel = streamConn.getSinkChannel();
            ChannelActiveListener writeListener = new ChannelActiveListener();
            DelegatingStreamSinkConduit conduitWrapper = new DelegatingStreamSinkConduit(sinkChannel.getConduit(), writeListener);
            sinkChannel.setConduit(conduitWrapper);

            ConduitStreamSourceChannel sourceChannel = streamConn.getSourceChannel();
            DelegatingStreamSourceConduit srcConduitWrapper = new DelegatingStreamSourceConduit(sourceChannel.getConduit(), data -> checkPong(data));
            sourceChannel.setConduit(srcConduitWrapper);

            heartbeatExecutor.schedule(() -> sendPing(writeListener, streamConn), getHeartbeatRate(), TimeUnit.MILLISECONDS);
        }

        private void sendPing(ChannelActiveListener writeListener, StreamConnection streamConn) {
            if (writeListener.isActive(getHeartbeatRate())) {
                // active means that data was written to the channel in the least heartbeat interval
                // therefore we don't send a ping now to not cause collisions

                // reschedule ping
                heartbeatExecutor.schedule(() -> sendPing(writeListener, streamConn), getHeartbeatRate(), TimeUnit.MILLISECONDS);
                // mark as we received a heartbeat
                // heartbeatReceived(proxyId);
                return;
            }
            if (!streamConn.isOpen()) return;

            try {
                ((DelegatingStreamSinkConduit) streamConn.getSinkChannel().getConduit()).writeWithoutNotifying(ByteBuffer.wrap(WEBSOCKET_PING));
                streamConn.getSinkChannel().flush();
            } catch (IOException e) {
                // Ignore failure, keep trying as long as the stream connection is valid.
            }

            heartbeatExecutor.schedule(() -> sendPing(writeListener, streamConn), getHeartbeatRate(), TimeUnit.MILLISECONDS);
        }

        private void checkPong(byte[] response) {
            if (!(response.length > 0)) return;
            if (response[0] == WEBSOCKET_PONG) {
                // ignore websocket PING PONG (native heart beat)
                // heartbeatReceived(proxyId);
                return;
            }

            // payload length analyzer
            // https://datatracker.ietf.org/doc/html/rfc6455#section-5.2
            int payloadLength = response[1] & 0x7F;
            if (payloadLength == 126) {
                payloadLength = ByteBuffer.wrap(new byte[]{response[2], response[3]}).getShort() & 0xFFFF;
            } else if (payloadLength == 127) {
                // We don't calculate the exact payload length of such packet because its payload length greater than 65535 bytes.
                // Calculating it is meaningless.
                // If you really want to calculate it, use BigInteger(1, <byte array of response[2] to response[9]>)
                payloadLength = 65535 * 2;
            }

            Proxy proxy = proxyService.getProxy(proxyId);

            // if a proxy is terminated manually before status block created, stop checkPong.
            if (proxy == null || (proxy.getStatus() == ProxyStatus.Stopping || proxy.getStatus() == ProxyStatus.Stopped)) {
                websocketHeartbeats.remove(proxyId);
                return;
            }

            HeartbeatStatus heartbeatStatus = websocketHeartbeats.computeIfAbsent(proxyId, k -> new HeartbeatStatus());
            int lastLength = heartbeatStatus.getTotalPayloadLength();
            heartbeatStatus.setTotalPayloadLength(lastLength + payloadLength);
        }
    }

    private class InactiveProxyKiller implements Runnable {
        @Override
        public void run() {
            long cleanupInterval = getHeartbeatRate();
            long heartbeatTimeout = getHeartbeatTimeout();

            while (true) {
                if (enabled) {
                    try {
                        long currentTimestamp = System.currentTimeMillis();
                        for (Proxy proxy : proxyService.getProxies(null, true)) {
                            if (proxy.getStatus() != ProxyStatus.Up) continue;
                            else if (proxy.getSpec().getId().equals("filebrowser")) continue;

                            // reached max-age limitation
                            if (currentTimestamp - proxy.getStartupTimestamp() > engagementProperties.getMaxAge().toMillis()) {
                                log.info(String.format("Releasing timeout proxy [user: %s] [spec: %s] [id: %s] [duration: %dhr]", proxy.getUserId(), proxy.getSpec().getId(), proxy.getId(), engagementProperties.getMaxAge().toHours()));
                                proxyHeartbeats.remove(proxy.getId());
                                websocketHeartbeats.remove(proxy.getId());
                                proxyService.stopProxy(proxy, true, true);
                                continue;
                            }

                            // websocket idle termination
                            boolean isPureHttp = false;
                            boolean isIdled = false;
                            int idleRetryLimit = engagementProperties.getIdleRetry();
                            if (engagementProperties.isEnabled()) {
                                HeartbeatStatus heartbeatStatus = websocketHeartbeats.get(proxy.getId());

                                // 230 bytes per second default (10% load, 2300 bytes/sec when working on vscode)
                                int threshold = engagementProperties.getThreshold();

                                if (heartbeatStatus == null) {
                                    isPureHttp = true;
                                } else {
                                    long duration = currentTimestamp - heartbeatStatus.getStartRecordTimestamp();
                                    // idle
                                    double rate = heartbeatStatus.getTotalPayloadLength() / (duration / 1000.0);
                                    if (rate < threshold) {
                                        heartbeatStatus.increaseCounter();
                                        log.debug("proxy {} websocket idle detected ({}/{})! average speed={} bytes/sec, threshold={} bytes/sec", proxy.getId(), heartbeatStatus.getTerminateCounter(), idleRetryLimit, rate, threshold);
                                    }
                                    // active
                                    else {
                                        log.debug("proxy {} websocket active, average speed={} bytes/sec, threshold={} bytes/sec", proxy.getId(), rate, threshold);
                                        heartbeatStatus.clearAll();
                                    }

                                    // idle confirmed
                                    if (heartbeatStatus.getTerminateCounter() >= idleRetryLimit) {
                                        isIdled = true;
                                    }

                                    heartbeatStatus.setLastRecordTimestamp(System.currentTimeMillis());
                                }

                                Long lastHeartbeat = proxyHeartbeats.get(proxy.getId());
                                if (lastHeartbeat == null) lastHeartbeat = proxy.getStartupTimestamp();
                                long proxySilence = currentTimestamp - lastHeartbeat;
                                if ((proxySilence > heartbeatTimeout) && (isPureHttp | isIdled)) {
                                    long silence = isPureHttp ? proxySilence : cleanupInterval * (heartbeatStatus.getTerminateCounter() - 1);
                                    log.info("Releasing {} proxy [user: {}] [spec: {}] [id: {}] [silence: {}ms]",
                                        isPureHttp ? "inactive" : "idled",
                                        proxy.getUserId(),
                                        proxy.getSpec().getId(),
                                        proxy.getId(),
                                        silence);

                                    proxyHeartbeats.remove(proxy.getId());
                                    websocketHeartbeats.remove(proxy.getId());
                                    proxyService.stopProxy(proxy, true, true, silence);
                                }
                                log.debug("proxy {} received HTTP requests {} ms ago, threshold={} ms", proxy.getId(), proxySilence, heartbeatTimeout);
                            }
                        }
                    } catch (Throwable t) {
                        log.error("Error in " + this.getClass().getSimpleName(), t);
                    }
                }
                try {
                    Thread.sleep(cleanupInterval);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
