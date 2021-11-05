package eu.openanalytics.containerproxy.service;

import eu.openanalytics.containerproxy.log.ILogStorage;
import eu.openanalytics.containerproxy.log.NoopLogStorage;
import eu.openanalytics.containerproxy.model.runtime.Proxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

@Service
public class LogService {

    private static final String PARAM_STREAMS = "streams";
    final Environment environment;
    final ILogStorage logStorage;
    private final Logger log = LogManager.getLogger(LogService.class);
    private ExecutorService executor;
    private boolean loggingEnabled;

    public LogService(Environment environment, ILogStorage logStorage) {
        this.environment = environment;
        this.logStorage = logStorage;
    }

    @PostConstruct
    public void init() {
        try {
            logStorage.initialize();
            loggingEnabled = !(logStorage instanceof NoopLogStorage);
        } catch (IOException e) {
            log.error("Failed to initialize container log storage", e);
        }

        if (isLoggingEnabled()) {
            executor = Executors.newCachedThreadPool();
            log.info("Container logging enabled. Log files will be saved to " + logStorage.getStorageLocation());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) executor.shutdown();
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public void attachToOutput(Proxy proxy, BiConsumer<OutputStream, OutputStream> outputAttacher) {
        if (!isLoggingEnabled()) return;

        executor.submit(() -> {
            try {
                OutputStream[] streams = logStorage.createOutputStreams(proxy);
                if (streams == null || streams.length < 2) {
                    log.error("Failed to attach logging of proxy " + proxy.getId() + ": no output streams defined");
                } else {
                    proxy.getContainers().get(0).getParameters().put(PARAM_STREAMS, streams);
                    if (log.isDebugEnabled()) log.debug("Container logging started for proxy " + proxy.getId());
                    // Note that this call will block until the container is stopped.
                    outputAttacher.accept(streams[0], streams[1]);
                }
            } catch (Exception e) {
                log.error("Failed to attach logging of proxy " + proxy.getId(), e);
            }
            if (log.isDebugEnabled()) log.debug("Container logging ended for proxy " + proxy.getId());
        });
    }

    public void detach(Proxy proxy) {
        if (!isLoggingEnabled()) return;

        OutputStream[] streams = (OutputStream[]) proxy.getContainers().get(0).getParameters().get(PARAM_STREAMS);
        if (streams == null || streams.length < 2) {
            log.warn("Cannot detach container logging: streams not found");
            return;
        }
        for (int i = 0; i < streams.length; i++) {
            try {
                streams[i].flush();
                streams[i].close();
            } catch (IOException e) {
                log.error("Failed to close container logging streams", e);
            }
        }
    }

    public String[] getLogs(Proxy proxy) {
        if (!isLoggingEnabled()) return null;

        try {
            return logStorage.getLogs(proxy);
        } catch (IOException e) {
            log.error("Failed to locate logs for proxy " + proxy.getId(), e);
        }

        return null;
    }

}
