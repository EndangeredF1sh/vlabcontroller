package hk.edu.polyu.comp.vlabcontroller.backend;

import hk.edu.polyu.comp.vlabcontroller.backend.kubernetes.KubernetesBackend;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Objects;

import static io.vavr.API.unchecked;

@Service
@RequiredArgsConstructor
@RefreshScope
public class ContainerBackendFactory extends AbstractFactoryBean<IContainerBackend> implements ApplicationContextAware {
    @Setter(onMethod_ = {@Inject})
    private ApplicationContext applicationContext;
    @Setter(onMethod_ = {@Inject})
    private ProxyProperties proxyProperties;

    @Override
    public Class<?> getObjectType() {
        return IContainerBackend.class;
    }

    @Override
    protected IContainerBackend createInstance() throws Exception {
        var backendName =  proxyProperties.getContainerBackend();
        var backend = ContainerBackend.createFor(backendName);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(backend);
        backend.initialize();
        return backend;
    }

    private enum ContainerBackend {
        Kubernetes("kubernetes", KubernetesBackend.class);

        private final String name;
        private final Class<? extends IContainerBackend> type;

        ContainerBackend(String name, Class<? extends IContainerBackend> type) {
            this.name = name;
            this.type = type;
        }

        public static IContainerBackend createFor(String name) {
            return Arrays.stream(values())
                .filter(cb -> cb.name.equalsIgnoreCase(name)).map(unchecked(cb -> cb.type.newInstance()))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        }
    }
}
