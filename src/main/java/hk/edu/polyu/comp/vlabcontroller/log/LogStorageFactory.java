package hk.edu.polyu.comp.vlabcontroller.log;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service(value = "logStorage")
@Primary
@RequiredArgsConstructor
@RefreshScope
public class LogStorageFactory extends AbstractFactoryBean<ILogStorage> {
    private final ProxyProperties proxyProperties;
    private final ApplicationContext applicationContext;

    @Override
    public Class<?> getObjectType() {
        return ILogStorage.class;
    }

    @Override
    protected ILogStorage createInstance() throws Exception {
         ILogStorage storage;

        var containerLogPath = proxyProperties.getContainerLogPath();
        if (containerLogPath == null || containerLogPath.isBlank()) {
            storage = new NoopLogStorage();
        } else if (containerLogPath.toLowerCase().startsWith("s3://")) {
            storage = new S3LogStorage();
        } else {
            storage = new FileLogStorage();
        }

        applicationContext.getAutowireCapableBeanFactory().autowireBean(storage);
        return storage;
    }

}
