package hk.edu.polyu.comp.vlabcontroller.service;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.HeartbeatStatus;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyStatus;
import hk.edu.polyu.comp.vlabcontroller.util.ChannelActiveListener;
import hk.edu.polyu.comp.vlabcontroller.util.DelegatingStreamSinkConduit;
import hk.edu.polyu.comp.vlabcontroller.util.DelegatingStreamSourceConduit;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpServerConnection;
import io.vavr.Function0;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.xnio.StreamConnection;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
@RefreshScope
public class HeartbeatService {
    private static final byte[] WEBSOCKET_PING = {(byte) 0b10001001, (byte) 0b00000000};
    private static final byte WEBSOCKET_PONG = (byte) 0b10001010;

    @Getter
    private final Map<String, Duration> proxyHeartbeats = Collections.synchronizedMap(new HashMap<>());
    @Getter
    private final Map<String, HeartbeatStatus> websocketHeartbeats = Collections.synchronizedMap(new HashMap<>());

    @Setter
    private volatile boolean enabled;

    private final ProxyService proxyService;
    private final ProxyProperties proxyProperties;
    private final ThreadPoolTaskScheduler taskScheduler;

    private List<ScheduledFuture<?>> runningFutures = new ArrayList<>();

    private ScheduledFuture<?> idleDetectionFuture;

    @EventListener
    public void onRefreshScopeRefreshed(final RefreshScopeRefreshedEvent event) {
        log.debug("heartbeat service refreshed");
    }

    @PostConstruct
    public void init() {
        enabled = proxyProperties.isHeartbeatEnabled() || DurationUtils.isPositive(proxyProperties.getHeartbeatRate()) || DurationUtils.isPositive(proxyProperties.getHeartbeatTimeout());

        Runnable idleDetection = () -> {
            try {
                log.debug("running idle detection");
                var currentTimestamp = Duration.ofMillis(System.currentTimeMillis());
                proxyService.getProxies(null, true).stream()
                        .filter(proxy -> proxy.getStatus() == ProxyStatus.Up)
                        .filter(proxy -> !proxy.getSpec().getId().equals("filebrowser"))
                        .forEach(proxy -> {
                            var id = proxy.getId();
                            Consumer<Duration> deleteProxy = time -> {
                                proxyHeartbeats.remove(id);
                                websocketHeartbeats.remove(id);
                                proxyService.stopProxy(proxy, true, true, time);
                            };

                            var engagement = proxyProperties.getEngagement();
                            if (currentTimestamp.minus(proxy.getStartupTimestamp()).compareTo(engagement.getMaxAge()) > 0) {
                                log.info(String.format("Releasing timeout proxy [user: %s] [spec: %s] [id: %s] [duration: %dhr]", proxy.getUserId(), proxy.getSpec().getId(), id, engagement.getMaxAge().toHours()));
                                deleteProxy.accept(Duration.ZERO);
                                return;
                            }
                            // websocket idle termination
                            if (!engagement.isEnabled()) {
                                return;
                            }
                            var idleRetryLimit = engagement.getIdleRetry();
                            var webSocketHeartbeatStatus = websocketHeartbeats.get(id);
                            var isPureHttp = webSocketHeartbeatStatus == null;
                            Function0<Boolean> isIdled = () -> webSocketHeartbeatStatus.getTerminateCounter() >= idleRetryLimit;

                            // 230 bytes per second default (10% load, 2300 bytes/sec when working on vscode)
                            var threshold = engagement.getThreshold();

                            if (!isPureHttp) {
                                // idle
                                var duration = currentTimestamp.minus(webSocketHeartbeatStatus.getStartRecordTimestamp());
                                var rate = webSocketHeartbeatStatus.getTotalPayloadLength() / duration.toSeconds();
                                if (rate < threshold) {
                                    webSocketHeartbeatStatus.increaseCounter();
                                    log.debug("proxy {} websocket idle detected ({}/{})! average speed={} bytes/sec, threshold={} bytes/sec", id, webSocketHeartbeatStatus.getTerminateCounter(), idleRetryLimit, rate, threshold);
                                }
                                // active
                                else {
                                    log.debug("proxy {} websocket active, average speed={} bytes/sec, threshold={} bytes/sec", id, rate, threshold);
                                    webSocketHeartbeatStatus.clearAll();
                                }

                                webSocketHeartbeatStatus.setLastRecordTimestamp(Duration.ofMillis(System.currentTimeMillis()));
                            }

                            var proxySilence = currentTimestamp.minus(Optional.ofNullable(proxyHeartbeats.get(id)).orElseGet(proxy::getStartupTimestamp));
                            if ((proxySilence.compareTo(proxyProperties.getHeartbeatTimeout()) > 0) && (isPureHttp || isIdled.apply())) {
                                var silence = isPureHttp ? proxySilence : proxyProperties.getHeartbeatRate().multipliedBy(webSocketHeartbeatStatus.getTerminateCounter() - 1);
                                log.info("Releasing {} proxy [user: {}] [spec: {}] [id: {}] [silence: {}ms]",
                                        isPureHttp ? "inactive" : "idled",
                                        proxy.getUserId(),
                                        proxy.getSpec().getId(),
                                        id,
                                        silence);
                                deleteProxy.accept(silence);
                            }
                            log.debug("proxy {} received HTTP requests {} ms ago, inactive threshold={} ms", id, proxySilence, proxyProperties.getHeartbeatTimeout());
                        });
            } catch (Throwable t) {
                log.error("Error in " + this.getClass().getSimpleName(), t);
            }
        };

        if (idleDetectionFuture != null) {
            idleDetectionFuture.cancel(true);
            idleDetectionFuture = null;
        }

        if (enabled) {
            log.debug("Idle detection enabled");
            idleDetectionFuture = taskScheduler.scheduleAtFixedRate(idleDetection, proxyProperties.getHeartbeatRate());
        }
    }

    public void attachHeartbeatChecker(HttpServerExchange exchange, String proxyId) {
        if (exchange.isUpgrade()) {
            // For websockets, attach a ping-pong listener to the underlying TCP channel.
            var connector = new HeartbeatConnector(proxyId);
            // Delay the wrapping, because Undertow will make changes to the channel while the upgrade is being performed.
            var httpConn = (HttpServerConnection) exchange.getConnection();
            runningFutures.add(taskScheduler.scheduleAtFixedRate(() -> connector.wrapChannels(httpConn.getChannel()), Duration.ofSeconds(3)));
        } else {
            // request URI prefix filter
            // exchange.getRequestPath() == /proxy_endpoint/<proxy_uuid>/<real-path-in-container>
            // exchange.getRelativePath() == /<real-path-in-container>
            // e.g access http://<domain-name>/<spring-context-path>/app/app_name/static/js/example.js
            // exchange.getRequestPath() == /proxy_endpoint/<proxy-uuid>/static/js/example.js
            // exchange.getRelativePath() == /static/js/example.js
            for (var path : proxyProperties.getEngagement().getFilterPath()) {
                var relativeRequestPath = exchange.getRelativePath();
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
        var proxy = proxyService.getProxy(proxyId);
        if (log.isDebugEnabled()) log.debug("Heartbeat received for proxy " + proxyId);
        if (proxy != null) proxyHeartbeats.put(proxyId, Duration.ofMillis(System.currentTimeMillis()));
    }

    private class HeartbeatConnector {

        private final String proxyId;

        public HeartbeatConnector(String proxyId) {
            this.proxyId = proxyId;
        }

        private void wrapChannels(StreamConnection streamConn) {
            if (!streamConn.isOpen()) return;

            var sinkChannel = streamConn.getSinkChannel();
            var writeListener = new ChannelActiveListener();
            var conduitWrapper = new DelegatingStreamSinkConduit(sinkChannel.getConduit(), writeListener);
            sinkChannel.setConduit(conduitWrapper);

            var sourceChannel = streamConn.getSourceChannel();
            var srcConduitWrapper = new DelegatingStreamSourceConduit(sourceChannel.getConduit(), this::checkPong);
            sourceChannel.setConduit(srcConduitWrapper);

            runningFutures.add(taskScheduler.scheduleAtFixedRate(() -> sendPing(writeListener, streamConn), proxyProperties.getHeartbeatRate()));
        }

        private void sendPing(ChannelActiveListener writeListener, StreamConnection streamConn) {
            if (writeListener.isActive(proxyProperties.getHeartbeatRate())) {
                // active means that data was written to the channel in the least heartbeat interval
                // therefore we don't send a ping now to not cause collisions

                // reschedule ping
                runningFutures.add(taskScheduler.scheduleAtFixedRate(() -> sendPing(writeListener, streamConn), proxyProperties.getHeartbeatRate()));
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

            runningFutures.add(taskScheduler.scheduleAtFixedRate(() -> sendPing(writeListener, streamConn), proxyProperties.getHeartbeatRate()));
        }

        private void checkPong(byte[] response) {
            if (response.length <= 2) return;
            if (response[0] == WEBSOCKET_PONG) {
                // ignore websocket PING PONG (native heart beat)
                // heartbeatReceived(proxyId);
                return;
            }

            // payload length analyzer
            // https://datatracker.ietf.org/doc/html/rfc6455#section-5.2
            var payloadLength = response[1] & 0x7F;
            if (payloadLength == 126) {
                if (response.length < 4) {
                    // handle broken packet
                    payloadLength = 0;
                    log.debug("broken packet");
                } else {
                    payloadLength = ByteBuffer.wrap(new byte[]{response[2], response[3]}).getShort() & 0xFFFF;
                }
            } else if (payloadLength == 127) {
                if (response.length < 10) {
                    // handle broken packet
                    payloadLength = 0;
                    log.debug("broken huge packet");
                } else {
                    // We don't calculate the exact payload length of such packet because its payload length greater than 65535 bytes.
                    // Calculating it is meaningless.
                    // If you really want to calculate it, use BigInteger(1, <byte array of response[2] to response[9]>)
                    payloadLength = 65535 * 2;
                }
            }
            log.debug("Websocket packet received, length={} bytes", payloadLength);

            var proxy = proxyService.getProxy(proxyId);

            // if a proxy is terminated manually before status block created, stop checkPong.
            if (proxy == null || (proxy.getStatus() == ProxyStatus.Stopping || proxy.getStatus() == ProxyStatus.Stopped)) {
                websocketHeartbeats.remove(proxyId);
                return;
            }

            var heartbeatStatus = websocketHeartbeats.computeIfAbsent(proxyId, k -> new HeartbeatStatus());
            var lastLength = heartbeatStatus.getTotalPayloadLength();
            heartbeatStatus.setTotalPayloadLength(lastLength + payloadLength);
        }
    }
}
