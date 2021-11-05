package eu.openanalytics.containerproxy.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
public class CustomFirewallSecurityConfig implements ICustomSecurityConfig {

    private final Logger log = LogManager.getLogger(CustomFirewallSecurityConfig.class);

    @Override
    public void apply(WebSecurity web) throws Exception {
        log.info("Enable customized firewall");
        ICustomSecurityConfig.super.apply(web);
        StrictHttpFirewall customStrictHttpFirewall = new StrictHttpFirewall();
        customStrictHttpFirewall.setAllowUrlEncodedDoubleSlash(true);
        web.httpFirewall(customStrictHttpFirewall);
    }

    @Override
    public void apply(HttpSecurity http) throws Exception {
        ICustomSecurityConfig.super.apply(http);
    }
}
