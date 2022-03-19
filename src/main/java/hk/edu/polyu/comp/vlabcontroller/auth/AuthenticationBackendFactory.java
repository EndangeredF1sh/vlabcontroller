package hk.edu.polyu.comp.vlabcontroller.auth;

import hk.edu.polyu.comp.vlabcontroller.auth.impl.*;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.vavr.API.*;

/**
 * Instantiates an appropriate authentication backend depending on the application configuration.
 */
@Service(value = "authenticationBackend")
@Primary
@RequiredArgsConstructor
@RefreshScope
public class AuthenticationBackendFactory extends AbstractFactoryBean<IAuthenticationBackend> {
    private final ProxyProperties proxyProperties;
    private final ApplicationContext applicationContext;
    // These backends register some beans of their own, so must be instantiated here.
    private final KeycloakAuthenticationBackend keycloakBackend;

    @Override
    public Class<?> getObjectType() {
        return IAuthenticationBackend.class;
    }

    @Override
    protected IAuthenticationBackend createInstance() {
        var regBeans = new AtomicBoolean(true);
        var backend = Match(proxyProperties.getAuthentication()).of(
            Case($(NoAuthenticationBackend.NAME), NoAuthenticationBackend::new),
            Case($(SimpleAuthenticationBackend.NAME), SimpleAuthenticationBackend::new),
            Case($(OpenIDAuthenticationBackend.NAME), OpenIDAuthenticationBackend::new),
            Case($(WebServiceAuthenticationBackend.NAME), WebServiceAuthenticationBackend::new),
            Case($(KeycloakAuthenticationBackend.NAME), () -> {
                regBeans.set(false);
                return keycloakBackend;
            }),
            Case($(), type -> {
                throw new RuntimeException("Unknown authentication type:" + type);
            })
        );
        if (regBeans.get()) applicationContext.getAutowireCapableBeanFactory().autowireBean(backend);
        return backend;
    }

}
