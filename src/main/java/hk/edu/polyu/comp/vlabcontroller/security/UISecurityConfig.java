package hk.edu.polyu.comp.vlabcontroller.security;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UISecurityConfig implements ICustomSecurityConfig {
    private final ProxyService proxyService;
    private final IAuthenticationBackend auth;
    private final UserService userService;

    @Override
    public void apply(HttpSecurity http) throws Exception {
        if (auth.hasAuthorization()) {
            // Limit access to the app pages according to spec permissions
            var convertedMatches = proxyService.getProxySpecs(null, true).stream()
                .filter(x -> !x.getAccessGroups().isEmpty())
                .collect(Collectors.toMap(ProxySpec::getId, x -> x.getAccessGroups().stream().map(String::toUpperCase)));
            for (var entry : convertedMatches.entrySet()) {
                http.authorizeRequests().antMatchers("/app/" + entry.getKey()).hasAnyRole(entry.getValue().toArray(String[]::new));
            }

            // Limit access to the admin pages
            http.authorizeRequests().antMatchers("/admin").hasAnyRole(userService.getAdminGroups().toArray(String[]::new));
        }
    }
}
