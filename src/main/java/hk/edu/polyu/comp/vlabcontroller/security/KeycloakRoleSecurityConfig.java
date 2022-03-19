package hk.edu.polyu.comp.vlabcontroller.security;

import com.google.common.base.Strings;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "proxy.authentication", havingValue = "keycloak")
@RequiredArgsConstructor
@RefreshScope
public class KeycloakRoleSecurityConfig implements ICustomSecurityConfig {
    private final ProxyProperties proxyProperties;

    @Override
    public void apply(WebSecurity web) throws Exception {
        ICustomSecurityConfig.super.apply(web);
    }

    @Override
    public void apply(HttpSecurity http) throws Exception {
        ICustomSecurityConfig.super.apply(http);
        var uriArray = new String[]{"/api/**", "/app/**", "/app_direct/**", "/filebrowser", "/controlpanel",  proxyProperties.getLandingPage()};
        var role = proxyProperties.getAllowedRole();
        if (!Strings.isNullOrEmpty(role)) {
            log.info("Enable allowed roles mode");
            http.authorizeRequests().antMatchers("/").anonymous();
            http.authorizeRequests().antMatchers(uriArray).hasAnyRole(role.toUpperCase());
        }
    }

}
