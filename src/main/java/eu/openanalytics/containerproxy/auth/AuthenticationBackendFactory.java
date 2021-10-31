package eu.openanalytics.containerproxy.auth;

import eu.openanalytics.containerproxy.auth.impl.*;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Instantiates an appropriate authentication backend depending on the application configuration.
 */
@Service(value = "authenticationBackend")
@Primary
public class AuthenticationBackendFactory extends AbstractFactoryBean<IAuthenticationBackend> {
  
  @Inject
  private Environment environment;
  
  @Inject
  private ApplicationContext applicationContext;
  
  // These backends register some beans of their own, so must be instantiated here.
  
  @Inject
  private KeycloakAuthenticationBackend keycloakBackend;
  
  @Override
  public Class<?> getObjectType() {
    return IAuthenticationBackend.class;
  }
  
  @Override
  protected IAuthenticationBackend createInstance() throws Exception {
    IAuthenticationBackend backend = null;
    
    String type = environment.getProperty("proxy.authentication", "none");
    switch (type) {
      case NoAuthenticationBackend.NAME:
        backend = new NoAuthenticationBackend();
        break;
      case SimpleAuthenticationBackend.NAME:
        backend = new SimpleAuthenticationBackend();
        break;
      case OpenIDAuthenticationBackend.NAME:
        backend = new OpenIDAuthenticationBackend();
        break;
      case KeycloakAuthenticationBackend.NAME:
        return keycloakBackend;
      case WebServiceAuthenticationBackend.NAME:
        backend = new WebServiceAuthenticationBackend();
        break;
    }
    if (backend == null) throw new RuntimeException("Unknown authentication type:" + type);
    
    applicationContext.getAutowireCapableBeanFactory().autowireBean(backend);
    return backend;
  }
  
}
