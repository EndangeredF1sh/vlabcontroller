package hk.edu.polyu.comp.vlabcontroller.log;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class AbstractLogStorage implements ILogStorage {

    private static final String PARAM_LOG_PATHS = "log_paths";

    @Inject
    protected Environment environment;

    protected String containerLogPath;

    @Override
    public void initialize() throws IOException {
        containerLogPath = environment.getProperty("proxy.container-log-path");
    }

    @Override
    public String getStorageLocation() {
        return containerLogPath;
    }

    @Override
    public String[] getLogs(Proxy proxy) throws IOException {
        String[] paths = (String[]) proxy.getContainerGroup().getParameters().get(PARAM_LOG_PATHS);
        if (paths == null) {
            String timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
            paths = new String[]{
                    String.format("%s/%s_%s_%s_stdout.log", containerLogPath, proxy.getSpec().getId(), proxy.getId(), timestamp),
                    String.format("%s/%s_%s_%s_stderr.log", containerLogPath, proxy.getSpec().getId(), proxy.getId(), timestamp)
            };
            proxy.getContainerGroup().getParameters().put(PARAM_LOG_PATHS, paths);
        }
        return paths;
    }
}
