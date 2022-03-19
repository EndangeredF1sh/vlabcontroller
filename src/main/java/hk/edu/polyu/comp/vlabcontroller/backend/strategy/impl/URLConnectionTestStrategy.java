package hk.edu.polyu.comp.vlabcontroller.backend.strategy.impl;

import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyTestStrategy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.function.IntPredicate;

/**
 * This component tests the responsiveness of containers by making an HTTP GET request to the container's published port (default 3838).
 * If this request does not receive non-error (5xx) response within a configured time limit, the container is considered to be unresponsive.
 */
@Component
@Primary
public class URLConnectionTestStrategy implements IProxyTestStrategy {

    private final Environment environment;
    private final Logger log = LogManager.getLogger(URLConnectionTestStrategy.class);

    public URLConnectionTestStrategy(Environment environment) {
        this.environment = environment;
    }

    private static boolean retry(IntPredicate job, int tries, int waitTime, boolean retryOnException) {
        boolean retVal = false;
        RuntimeException exception = null;
        for (int currentTry = 1; currentTry <= tries; currentTry++) {
            try {
                if (job.test(currentTry)) {
                    retVal = true;
                    exception = null;
                    break;
                }
            } catch (RuntimeException e) {
                if (retryOnException) exception = e;
                else throw e;
            }
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ignore) {
            }
        }
        if (exception == null) return retVal;
        else throw exception;
    }

    @Override
    public boolean testProxy(Proxy proxy) {

        int totalWaitMs = Integer.parseInt(environment.getProperty("proxy.container-wait-time", "20000"));
        int waitMs = Math.min(2000, totalWaitMs);
        int maxTries = totalWaitMs / waitMs;
        int timeoutMs = Integer.parseInt(environment.getProperty("proxy.container-wait-timeout", "5000"));

        if (proxy.getTargets().isEmpty()) return false;
        URI targetURI = proxy.getTargets().values().iterator().next();
        int failedResponseCode = -1;
        return retry(i -> {
            try {
                if (proxy.getStatus() == ProxyStatus.Stopping || proxy.getStatus() == ProxyStatus.Stopped) return true;
                URL testURL = new URL(targetURI.toString());
                HttpURLConnection connection = ((HttpURLConnection) testURL.openConnection());
                connection.setConnectTimeout(timeoutMs);
                connection.setInstanceFollowRedirects(false);
                int responseCode = connection.getResponseCode();
                if (responseCode < 500) return true;
            } catch (Exception e) {
                if (i > 1)
                    log.warn(String.format("Container unresponsive, trying again (%d/%d): %s", i, maxTries, targetURI));
            }
            return false;
        }, maxTries, waitMs, false);
    }
}
