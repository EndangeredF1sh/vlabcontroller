package hk.edu.polyu.comp.vlabcontroller.auth.impl;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.auth.impl.keycloak.AuthenticationFailureHandler;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.RequiredArgsConstructor;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.AdapterDeploymentContextFactoryBean;
import org.keycloak.adapters.springsecurity.account.KeycloakRole;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationEntryPoint;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationFailureHandler;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.authentication.KeycloakLogoutHandler;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.AuthorizedUrl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;

import static io.vavr.API.*;
@Component
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
@RefreshScope
public class KeycloakAuthenticationBackend implements IAuthenticationBackend {

    public static final String NAME = "keycloak";

    final ProxyProperties proxyProperties;
    final WebSecurityConfigurerAdapter webSecurityConfigurerAdapter;
    final ApplicationContext ctx;
    final AuthenticationManager authenticationManager;

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
        http.formLogin().disable();

        http
                .sessionManagement().sessionAuthenticationStrategy(sessionAuthenticationStrategy())
                .and()
                .addFilterBefore(keycloakPreAuthActionsFilter(), LogoutFilter.class)
                .addFilterBefore(keycloakAuthenticationProcessingFilter(), BasicAuthenticationFilter.class)
                .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint())
                .and()
                .logout().addLogoutHandler(keycloakLogoutHandler());
    }

    @Override
    public void configureAuthenticationManagerBuilder(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(keycloakAuthenticationProvider());
    }

    @Override
    public String getLogoutSuccessURL() {
        return "/";
    }

    @Bean
    @ConditionalOnProperty(name = "proxy.authentication", havingValue = "keycloak")
    protected KeycloakAuthenticationProcessingFilter keycloakAuthenticationProcessingFilter() throws Exception {
        // Possible solution for issue #21037, create a custom RequestMatcher that doesn't include a QueryParamPresenceRequestMatcher(OAuth2Constants.ACCESS_TOKEN) request matcher.
        // The QueryParamPresenceRequestMatcher(OAuth2Constants.ACCESS_TOKEN) caused the HTTP requests to be changed before they were processed.
        // Because the HTTP requests are adapted before they are processed, the requested failed to complete successfully and caused an io.undertow.server.TruncatedResponseException
        // If in the future we need a RequestMatcher for het ACCESS_TOKEN, we can implement one ourselves
        RequestMatcher requestMatcher =
                new OrRequestMatcher(
                        new AntPathRequestMatcher(KeycloakAuthenticationEntryPoint.DEFAULT_LOGIN_URI),
                        new RequestHeaderRequestMatcher(KeycloakAuthenticationProcessingFilter.AUTHORIZATION_HEADER)
                );

        var filter = new KeycloakAuthenticationProcessingFilter(authenticationManager, requestMatcher);
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy());
        filter.setAuthenticationFailureHandler(keycloakAuthenticationFailureHandler());
        // Fix: call afterPropertiesSet manually, because Spring doesn't invoke it for some reason.
        filter.setApplicationContext(ctx);
        filter.afterPropertiesSet();
        return filter;
    }

    @Bean
    @ConditionalOnProperty(name = "proxy.authentication", havingValue = "keycloak")
    protected KeycloakPreAuthActionsFilter keycloakPreAuthActionsFilter() {
        var filter = new KeycloakPreAuthActionsFilter(httpSessionManager());
        // Fix: call afterPropertiesSet manually, because Spring doesn't invoke it for some reason.
        filter.setApplicationContext(ctx);
        try {
            filter.afterPropertiesSet();
        } catch (ServletException e) {
        }
        return filter;
    }

    @Bean
    @ConditionalOnProperty(name = "proxy.authentication", havingValue = "keycloak")
    protected HttpSessionManager httpSessionManager() {
        return new HttpSessionManager();
    }

    @Bean
    @ConditionalOnProperty(name = "proxy.authentication", havingValue = "keycloak")
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new CompositeSessionAuthenticationStrategy(Arrays.asList(
                new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl()),
                new ChangeSessionIdAuthenticationStrategy()
        ));
    }

    @Bean
    @ConditionalOnProperty(name = "proxy.authentication", havingValue = "keycloak")
    public KeycloakAuthenticationFailureHandler keycloakAuthenticationFailureHandler() {
        return new AuthenticationFailureHandler();
    }

    @Bean
    @ConditionalOnProperty(name = "proxy.authentication", havingValue = "keycloak")
    protected AdapterDeploymentContext adapterDeploymentContext() throws Exception {
        var cfg = new AdapterConfig();
        var keycloak = proxyProperties.getKeycloak();

        cfg.setRealm(keycloak.getRealm());
        cfg.setAuthServerUrl(keycloak.getAuthServerUrl());
        cfg.setResource(keycloak.getResource());
        cfg.setSslRequired(keycloak.getSslRequired());
        cfg.setUseResourceRoleMappings(keycloak.isUseResourceRoleMappings());
        cfg.setCredentials(Map.of("secret", keycloak.getCredentialsSecret()));
        var dep = KeycloakDeploymentBuilder.build(cfg);
        var factoryBean = new AdapterDeploymentContextFactoryBean(facade -> dep);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    protected AuthenticationEntryPoint authenticationEntryPoint() throws Exception {
        return new KeycloakAuthenticationEntryPoint(adapterDeploymentContext());
    }

    protected KeycloakAuthenticationProvider keycloakAuthenticationProvider() {
        return new KeycloakAuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                var token = (KeycloakAuthenticationToken) super.authenticate(authentication);
                List<GrantedAuthority> auth = token.getAuthorities().stream()
                    .map(t -> t.getAuthority().toUpperCase())
                    .map(a -> a.startsWith("ROLE_") ? a : "ROLE_" + a)
                    .map(KeycloakRole::new)
                    .collect(Collectors.toList());
                var nameAttribute = proxyProperties.getKeycloak().getNameAttribute().toLowerCase();
                return new KeycloakAuthenticationToken2(token.getAccount(), token.isInteractive(), nameAttribute, auth);
            }
        };
    }

    protected KeycloakLogoutHandler keycloakLogoutHandler() throws Exception {
        return new KeycloakLogoutHandler(adapterDeploymentContext());
    }

    private static class KeycloakAuthenticationToken2 extends KeycloakAuthenticationToken implements Serializable {

        private static final long serialVersionUID = -521347733024996150L;

        private final String nameAttribute;

        public KeycloakAuthenticationToken2(KeycloakAccount account, boolean interactive, String nameAttribute, Collection<? extends GrantedAuthority> authorities) {
            super(account, interactive, authorities);
            this.nameAttribute = nameAttribute;
        }

        @Override
        public String getName() {
            var ctx = getAccount().getKeycloakSecurityContext();
            var token = Optional.ofNullable(ctx.getIdToken()).orElseGet(ctx::getToken);
            return Match(nameAttribute).of(
                Case($(IDToken.PREFERRED_USERNAME), token::getPreferredUsername),
                Case($(IDToken.NICKNAME), token::getNickName),
                Case($(IDToken.EMAIL), token::getEmail),
                Case($(), token::getName)
            );
        }
    }
}
