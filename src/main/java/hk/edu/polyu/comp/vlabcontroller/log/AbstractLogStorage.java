package hk.edu.polyu.comp.vlabcontroller.log;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import lombok.Setter;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RefreshScope
public abstract class AbstractLogStorage implements ILogStorage {

    private static final String PARAM_LOG_PATHS = "log_paths";

    @Setter(onMethod_ = {@Inject})
    protected ProxyProperties proxyProperties;

    protected String containerLogPath;

    @Override
    public void initialize() throws IOException {
        containerLogPath =  proxyProperties.getContainerLogPath();
    }

    @Override
    public String getStorageLocation() {
        return proxyProperties.getContainerLogPath();
    }

    @Override
    public String[] getLogs(Proxy proxy) throws IOException {
        var paths = (String[]) proxy.getContainerGroup().getParameters().get(PARAM_LOG_PATHS);
        if (paths == null) {
            var timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
            paths = new String[]{
                    String.format("%s/%s_%s_%s_stdout.log", getStorageLocation(), proxy.getSpec().getId(), proxy.getId(), timestamp),
                    String.format("%s/%s_%s_%s_stderr.log", getStorageLocation(), proxy.getSpec().getId(), proxy.getId(), timestamp)
            };
            proxy.getContainerGroup().getParameters().put(PARAM_LOG_PATHS, paths);
        }
        return paths;
    }
}
