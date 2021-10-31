package eu.openanalytics.containerproxy.ui;

import eu.openanalytics.containerproxy.api.BaseController;
import eu.openanalytics.containerproxy.auth.IAuthenticationBackend;
import eu.openanalytics.containerproxy.auth.impl.OpenIDAuthenticationBackend;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.inject.Inject;
import java.util.Optional;

@Controller
public class AuthController extends BaseController {
  
  @Inject
  private Environment environment;
  
  @Inject
  private IAuthenticationBackend auth;
  
  @RequestMapping(value = "/login", method = RequestMethod.GET)
  public Object getLoginPage(@RequestParam Optional<String> error, ModelMap map) {
    prepareMap(map);
    if (error.isPresent()) map.put("error", "Invalid user name or password");
    
    if (auth instanceof OpenIDAuthenticationBackend) {
      return new RedirectView(((OpenIDAuthenticationBackend) auth).getLoginRedirectURI());
    } else {
      return "login";
    }
  }
  
  @RequestMapping(value = "/auth-error", method = RequestMethod.GET)
  public String getAuthErrorPage(ModelMap map) {
    prepareMap(map);
    map.put("application_name", environment.getProperty("spring.application.name"));
    return "auth-error";
  }
  
  @RequestMapping(value = "/app-access-denied", method = RequestMethod.GET)
  public String getAppAccessDeniedPage(ModelMap map) {
    prepareMap(map);
    return "app-access-denied";
  }
  
  @RequestMapping(value = "/logout-success", method = RequestMethod.GET)
  public String getLogoutSuccessPage(ModelMap map) {
    prepareMap(map);
    return "logout-success";
  }
  
}
