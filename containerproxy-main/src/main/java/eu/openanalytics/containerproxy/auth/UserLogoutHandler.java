package eu.openanalytics.containerproxy.auth;

import eu.openanalytics.containerproxy.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class UserLogoutHandler implements LogoutHandler {
  
  @Inject
  private UserService userService;
  
  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    userService.logout(authentication);
  }
  
}
