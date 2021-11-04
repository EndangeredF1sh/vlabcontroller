package eu.openanalytics.containerproxy.backend;

import eu.openanalytics.containerproxy.backend.docker.DockerEngineBackend;
import eu.openanalytics.containerproxy.backend.docker.DockerSwarmBackend;
import eu.openanalytics.containerproxy.backend.kubernetes.KubernetesBackend;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class ContainerBackendFactory extends AbstractFactoryBean<IContainerBackend> implements ApplicationContextAware {
  
  private static final String PROPERTY_CONTAINER_BACKEND = "proxy.container-backend";
  protected final Environment environment;
  private ApplicationContext applicationContext;
  
  public ContainerBackendFactory(Environment environment) {
    this.environment = environment;
  }
  
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
  
  @Override
  public Class<?> getObjectType() {
    return IContainerBackend.class;
  }
  
  @Override
  protected IContainerBackend createInstance() throws Exception {
    String backendName = environment.getProperty(PROPERTY_CONTAINER_BACKEND);
    IContainerBackend backend = ContainerBackend.createFor(backendName);
    applicationContext.getAutowireCapableBeanFactory().autowireBean(backend);
    backend.initialize();
    return backend;
  }
  
  private enum ContainerBackend {
    DockerEngine("docker", DockerEngineBackend.class),
    DockerSwarm("docker-swarm", DockerSwarmBackend.class),
    Kubernetes("kubernetes", KubernetesBackend.class);
    
    private final String name;
    private final Class<? extends IContainerBackend> type;
    
    ContainerBackend(String name, Class<? extends IContainerBackend> type) {
      this.name = name;
      this.type = type;
    }
    
    public static IContainerBackend createFor(String name) throws Exception {
      for (ContainerBackend cb : values()) {
        if (cb.name.equalsIgnoreCase(name)) return cb.type.newInstance();
      }
      return DockerEngine.type.newInstance();
    }
  }
}
