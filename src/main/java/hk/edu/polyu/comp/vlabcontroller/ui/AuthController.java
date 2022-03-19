package hk.edu.polyu.comp.vlabcontroller.ui;

import hk.edu.polyu.comp.vlabcontroller.api.BaseController;
import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.auth.impl.OpenIDAuthenticationBackend;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController extends BaseController {
    private final IAuthenticationBackend auth;
    private final Environment environment;

    @GetMapping(value = "/login")
    public Object getLoginPage(@RequestParam Optional<String> error, ModelMap map) {
        prepareMap(map);
        if (error.isPresent()) map.put("error", "Invalid user name or password");

        if (auth instanceof OpenIDAuthenticationBackend) {
            return new RedirectView(((OpenIDAuthenticationBackend) auth).getLoginRedirectURI());
        } else {
            return "login";
        }
    }

    @GetMapping(value = "/auth-error")
    public String getAuthErrorPage(ModelMap map) {
        prepareMap(map);
        map.put("application_name", environment.getProperty("spring.application.name"));
        return "auth-error";
    }

    @GetMapping(value = "/app-access-denied")
    public String getAppAccessDeniedPage(ModelMap map) {
        prepareMap(map);
        return "app-access-denied";
    }

    @GetMapping(value = "/logout-success")
    public String getLogoutSuccessPage(ModelMap map) {
        prepareMap(map);
        return "logout-success";
    }

}
