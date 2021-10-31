package eu.openanalytics.containerproxy.auth;

import eu.openanalytics.containerproxy.model.spec.ContainerSpec;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.AuthorizedUrl;

import java.util.List;

public interface IAuthenticationBackend {
  
  /**
   * Get the name for this authentication backend, as used in the application.yml file.
   */
  String getName();
  
  /**
   * Return true if this authentication backend supports authorization.
   * In this context, authorization means the separation of permission levels
   * via groups.
   * <p>
   * If there is no authorization, all users have the same (administrator) permissions.
   */
  boolean hasAuthorization();
  
  /**
   * Perform customization on the http level, such as filters and login forms.
   */
  void configureHttpSecurity(HttpSecurity http, AuthorizedUrl anyRequestConfigurer) throws Exception;
  
  /**
   * Perform customization on the authentication manager level, such as authentication
   * handling and authority population.
   */
  void configureAuthenticationManagerBuilder(AuthenticationManagerBuilder auth) throws Exception;
  
  default String getLogoutSuccessURL() {
    return "/login";
  }
  
  default String getLogoutURL() {
    return "/logout";
  }
  
  default void customizeContainer(ContainerSpec spec) {
    // Default: do nothing.
  }
  
  default void customizeContainerEnv(List<String> env) {
    // Default: do nothing.
  }
  
}
