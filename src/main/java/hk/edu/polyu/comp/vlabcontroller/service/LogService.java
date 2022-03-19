package hk.edu.polyu.comp.vlabcontroller.service;

import hk.edu.polyu.comp.vlabcontroller.log.ILogStorage;
import hk.edu.polyu.comp.vlabcontroller.log.NoopLogStorage;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {
    private static final String PARAM_STREAMS = "streams";
    final ILogStorage logStorage;
    private ExecutorService executor;
    @Getter private boolean loggingEnabled;

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

    public void attachToOutput(Proxy proxy, BiConsumer<OutputStream, OutputStream> outputAttacher) {
        if (!isLoggingEnabled()) return;

        executor.submit(() -> {
            try {
                var streams = logStorage.createOutputStreams(proxy);
                if (streams == null || streams.length < 2) {
                    log.error("Failed to attach logging of proxy " + proxy.getId() + ": no output streams defined");
                } else {
                    proxy.getContainerGroup().getParameters().put(PARAM_STREAMS, streams);
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

        var streams = (OutputStream[]) proxy.getContainerGroup().getParameters().get(PARAM_STREAMS);
        if (streams == null || streams.length < 2) {
            log.warn("Cannot detach container logging: streams not found");
            return;
        }
        for (OutputStream stream : streams) {
            try {
                stream.flush();
                stream.close();
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
