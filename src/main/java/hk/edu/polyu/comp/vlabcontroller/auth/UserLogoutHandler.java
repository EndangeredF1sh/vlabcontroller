package hk.edu.polyu.comp.vlabcontroller.auth;

import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class UserLogoutHandler implements LogoutHandler {
    private final UserService userService;

    public UserLogoutHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        userService.logout(authentication);
    }

}
