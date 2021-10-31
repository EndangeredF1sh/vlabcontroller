package eu.openanalytics.containerproxy.security;

import com.google.common.base.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

import javax.inject.Inject;

@Configuration
@ConditionalOnProperty(name = "proxy.authentication", havingValue = "keycloak")
public class KeycloakRoleSecurityConfig implements ICustomSecurityConfig {
  private final Logger log = LogManager.getLogger(getClass());
  
  @Inject
  Environment environment;
  
  @Override
  public void apply(WebSecurity web) throws Exception {
    ICustomSecurityConfig.super.apply(web);
  }
  
  @Override
  public void apply(HttpSecurity http) throws Exception {
    ICustomSecurityConfig.super.apply(http);
    String[] uriArray = new String[]{"/api/**", "/app/**", "/app_direct/**", "/filebrowser", "/controlpanel", environment.getProperty("proxy.landing-page")};
    String role = environment.getProperty("proxy.allowed-role");
    if (!Strings.isNullOrEmpty(role)) {
      log.info("Enable allowed roles mode");
      http.authorizeRequests().antMatchers("/").anonymous();
      http.authorizeRequests().antMatchers(uriArray).hasAnyRole(role.toUpperCase());
    }
  }
  
}
