package hk.edu.polyu.comp.vlabcontroller.api;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.API.APIResponseBody;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import hk.edu.polyu.comp.vlabcontroller.service.UserService;
import hk.edu.polyu.comp.vlabcontroller.util.ProxyMappingManager;
import lombok.experimental.StandardException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class BaseController {

    @Autowired
    protected Environment environment;

    @Autowired
    protected ProxyService proxyService;

    @Autowired
    protected ProxyMappingManager mappingManager;

    @Autowired
    protected UserService userService;

    @Autowired
    protected IAuthenticationBackend authenticationBackend;

    protected void prepareMap(ModelMap map) {
        map.put("title", environment.getProperty("proxy.title", "VLabController"));
    }

    protected Proxy findUserProxy(String proxySpecId) {
        if (proxySpecId == null) return null;
        return proxyService.findProxy(p -> proxySpecId.equals(p.getSpec().getId()) && userService.isOwner(p), false);
    }

    @StandardException
    public static class NotFoundException extends RuntimeException {
        private static final long serialVersionUID = 2042632906716154791L;
    }

    @ControllerAdvice
    public static class RestErrorHandler {
        @ExceptionHandler(NotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        @ResponseBody
        public ResponseEntity<APIResponseBody<Object>> notFound(NotFoundException ex) {
            return APIResponseBody.notFound(ex.getMessage());
        }
    }
}
