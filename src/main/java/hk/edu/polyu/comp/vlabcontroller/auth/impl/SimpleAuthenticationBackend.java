package hk.edu.polyu.comp.vlabcontroller.auth.impl;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.Setter;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.AuthorizedUrl;

import javax.inject.Inject;

/**
 * Simple authentication method where user/password combinations are
 * provided by the application.yml file.
 */
@RefreshScope
public class SimpleAuthenticationBackend implements IAuthenticationBackend {

    public static final String NAME = "simple";

    @Setter(onMethod_ = {@Inject})
    private ProxyProperties proxyProperties;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean hasAuthorization() {
        return true;
    }

    @Override
    public void configureHttpSecurity(HttpSecurity http, AuthorizedUrl anyRequestConfigurer) throws Exception {
        // Nothing to do.
    }

    @Override
    public void configureAuthenticationManagerBuilder(AuthenticationManagerBuilder auth) throws Exception {
        var userDetails = auth.inMemoryAuthentication();
        proxyProperties.getUsers().stream()
            .filter(x -> x.getName() != null)
            .forEach(user -> userDetails
                .withUser(user.getName())
                .password("{noop}" + user.getPassword())
                .roles(user.getGroups().stream().map(String::toUpperCase).toArray(String[]::new)));
    }
}
