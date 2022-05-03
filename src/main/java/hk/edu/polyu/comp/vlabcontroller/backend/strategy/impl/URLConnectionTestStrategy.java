package hk.edu.polyu.comp.vlabcontroller.backend.strategy.impl;

import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyTestStrategy;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyStatus;
import hk.edu.polyu.comp.vlabcontroller.util.DurationUtil;
import hk.edu.polyu.comp.vlabcontroller.util.Retrying;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import static io.vavr.API.unchecked;

/**
 * This component tests the responsiveness of containers by making an HTTP GET request to the container's published port (default 3838).
 * If this request does not receive non-error (5xx) response within a configured time limit, the container is considered to be unresponsive.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
@RefreshScope
public class URLConnectionTestStrategy implements IProxyTestStrategy {
    private final ProxyProperties proxyProperties;
    private final Retrying retrying;

    @Override
    public boolean testProxy(Proxy proxy) {
        var totalWaitMs = proxyProperties.getContainerWaitTime();

        var waitMs = DurationUtil.atLeast(Duration.ofSeconds(2)).apply(totalWaitMs);
        var maxTries = (int) totalWaitMs.dividedBy(waitMs);
        var timeoutMs = proxyProperties.getContainerWaitTimeout();
        return Optional.ofNullable(proxy.getTargets())
            .map(x -> x.values().iterator().next())
            .map(x -> retrying.retry(i -> {
                try {
                    if (Stream.of(ProxyStatus.Stopping, ProxyStatus.Stopped).anyMatch(y -> y == proxy.getStatus())) return true;
                    var connection = (HttpURLConnection) new URL(x.toString()).openConnection();
                    connection.setConnectTimeout((int) timeoutMs.toMillis());
                    connection.setInstanceFollowRedirects(false);
                    var responseCode = connection.getResponseCode();
                    if (responseCode < 500) return true;
                } catch (Exception e) {
                    if (i > 1)
                        log.warn(String.format("Container unresponsive, trying again (%d/%d): %s", i, maxTries, x));
                }
                return false;
            }, maxTries, waitMs, false))
            .map(unchecked(x -> x.get()))
            .orElse(false);
    }
}
