package eu.openanalytics.containerproxy.auth;

import eu.openanalytics.containerproxy.auth.impl.*;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Instantiates an appropriate authentication backend depending on the application configuration.
 */
@Service(value = "authenticationBackend")
@Primary
public class AuthenticationBackendFactory extends AbstractFactoryBean<IAuthenticationBackend> {
    private final Environment environment;
    private final ApplicationContext applicationContext;
    // These backends register some beans of their own, so must be instantiated here.
    private final KeycloakAuthenticationBackend keycloakBackend;

    public AuthenticationBackendFactory(Environment environment, ApplicationContext applicationContext, KeycloakAuthenticationBackend keycloakBackend) {
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.keycloakBackend = keycloakBackend;
    }

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
            default:
                throw new RuntimeException("Unknown authentication type:" + type);
        }

        applicationContext.getAutowireCapableBeanFactory().autowireBean(backend);
        return backend;
    }

}
