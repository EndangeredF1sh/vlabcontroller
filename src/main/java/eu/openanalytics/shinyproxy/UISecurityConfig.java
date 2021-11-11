package eu.openanalytics.shinyproxy;

import eu.openanalytics.containerproxy.auth.IAuthenticationBackend;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.security.ICustomSecurityConfig;
import eu.openanalytics.containerproxy.service.ProxyService;
import eu.openanalytics.containerproxy.service.UserService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UISecurityConfig implements ICustomSecurityConfig {

    private final ProxyService proxyService;
    private final IAuthenticationBackend auth;
    private final UserService userService;

    public UISecurityConfig(ProxyService proxyService, IAuthenticationBackend auth, UserService userService) {
        this.proxyService = proxyService;
        this.auth = auth;
        this.userService = userService;
    }

    @Override
    public void apply(HttpSecurity http) throws Exception {
        if (auth.hasAuthorization()) {

            // Limit access to the app pages according to spec permissions
            for (ProxySpec spec : proxyService.getProxySpecs(null, true)) {
                List<String> groups = spec.getAccessGroups();
                if (groups.isEmpty()) continue;
                String[] appGroups = groups.stream().map(String::toUpperCase).toArray(String[]::new);
                http.authorizeRequests().antMatchers("/app/" + spec.getId()).hasAnyRole(appGroups);
            }

            // Limit access to the admin pages
            http.authorizeRequests().antMatchers("/admin").hasAnyRole(userService.getAdminGroups());
        }
    }
}
