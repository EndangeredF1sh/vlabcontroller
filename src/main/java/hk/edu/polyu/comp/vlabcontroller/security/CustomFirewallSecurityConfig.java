package hk.edu.polyu.comp.vlabcontroller.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Slf4j
@Configuration
public class CustomFirewallSecurityConfig implements ICustomSecurityConfig {
    @Override
    public void apply(WebSecurity web) throws Exception {
        log.info("Enable customized firewall");
        ICustomSecurityConfig.super.apply(web);
        var customStrictHttpFirewall = new StrictHttpFirewall();
        customStrictHttpFirewall.setAllowUrlEncodedDoubleSlash(true);
        web.httpFirewall(customStrictHttpFirewall);
    }

    @Override
    public void apply(HttpSecurity http) throws Exception {
        ICustomSecurityConfig.super.apply(http);
    }
}
