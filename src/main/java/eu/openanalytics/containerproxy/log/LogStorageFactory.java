package eu.openanalytics.containerproxy.log;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service(value = "logStorage")
@Primary
public class LogStorageFactory extends AbstractFactoryBean<ILogStorage> {
    private final Environment environment;
    private final ApplicationContext applicationContext;

    public LogStorageFactory(Environment environment, ApplicationContext applicationContext) {
        this.environment = environment;
        this.applicationContext = applicationContext;
    }

    @Override
    public Class<?> getObjectType() {
        return ILogStorage.class;
    }

    @Override
    protected ILogStorage createInstance() throws Exception {
        ILogStorage storage = null;

        String containerLogPath = environment.getProperty("proxy.container-log-path");
        if (containerLogPath == null || containerLogPath.trim().isEmpty()) {
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
