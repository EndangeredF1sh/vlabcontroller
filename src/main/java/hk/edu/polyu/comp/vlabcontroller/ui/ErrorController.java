package hk.edu.polyu.comp.vlabcontroller.ui;

import hk.edu.polyu.comp.vlabcontroller.api.BaseController;
import hk.edu.polyu.comp.vlabcontroller.auth.impl.keycloak.AuthenticationFailureHandler;
import lombok.extern.log4j.Log4j2;
import org.keycloak.adapters.OIDCAuthenticationError;
import org.keycloak.adapters.springsecurity.authentication.KeycloakCookieBasedRedirect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.NestedServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/error")
public class ErrorController extends BaseController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping(produces = "text/html")
    public String handleError(ModelMap map, HttpServletRequest request, HttpServletResponse response) {

        // handle keycloak errors
        Object obj = request.getSession().getAttribute(AuthenticationFailureHandler.SP_KEYCLOAK_ERROR_REASON);
        if (obj instanceof OIDCAuthenticationError.Reason) {
            request.getSession().removeAttribute(AuthenticationFailureHandler.SP_KEYCLOAK_ERROR_REASON);
            OIDCAuthenticationError.Reason reason = (OIDCAuthenticationError.Reason) obj;
            if (reason == OIDCAuthenticationError.Reason.INVALID_STATE_COOKIE ||
                    reason == OIDCAuthenticationError.Reason.STALE_TOKEN) {
                // These errors are typically caused by users using wrong bookmarks (e.g. bookmarks with states in)
                // or when some cookies got stale. However, the user is logged into the IDP, therefore it's enough to
                // send the user to the main page, and they will get logged in automatically.
                response.addCookie(KeycloakCookieBasedRedirect.createCookieFromRedirectUrl((String) null));
                return "redirect:/";
            } else {
                return "redirect:/auth-error";
            }
        }

        Throwable exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
        if (exception == null) {
            exception = (Throwable) request.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        }

        String[] msg = createMsgStack(exception);
        if (exception == null) {
            msg[0] = HttpStatus.valueOf(response.getStatus()).getReasonPhrase();
        }

        if (response.getStatus() == 200 && isAccountStatusException(exception)) {
            return "redirect:/";
        }

        if (isIllegalStateException(exception)) {
            log.warn("No state cookie on login attempt, force redirect to homepage");
            response.addCookie(KeycloakCookieBasedRedirect.createCookieFromRedirectUrl((String) null));
            return "redirect:/";
        }

        prepareMap(map);
        map.put("message", msg[0]);
        map.put("stackTrace", msg[1]);
        map.put("status", response.getStatus());

        return "error";
    }

    @RequestMapping(consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request, HttpServletResponse response) {
        Throwable exception = (Throwable) request.getAttribute("javax.servlet.error.exception");
        String[] msg = createMsgStack(exception);

        Map<String, Object> map = new HashMap<>();
        map.put("message", msg[0]);
        map.put("stackTrace", msg[1]);

        return new ResponseEntity<>(map, HttpStatus.valueOf(response.getStatus()));
    }

    public String getErrorPath() {
        return "/error";
    }

    private String[] createMsgStack(Throwable exception) {
        String message = "";
        String stackTrace = "";

        if (exception instanceof NestedServletException && exception.getCause() instanceof Exception) {
            exception = exception.getCause();
        }
        if (exception != null) {
            if (exception.getMessage() != null) message = exception.getMessage();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (PrintWriter writer = new PrintWriter(bos)) {
                exception.printStackTrace(writer);
            }
            stackTrace = bos.toString();
            stackTrace = stackTrace.replace(System.getProperty("line.separator"), "<br/>");
        }

        if (message.isEmpty()) message = "An unexpected server error occurred";
        if (stackTrace.isEmpty()) stackTrace = "n/a";

        return new String[]{message, stackTrace};
    }

    private boolean isAccountStatusException(Throwable exception) {
        if (exception == null) return false;
        if (exception instanceof AccountStatusException) return true;
        if (exception.getCause() != null) return isAccountStatusException(exception.getCause());
        return false;
    }

    private boolean isIllegalStateException(Throwable exception) {
        if (exception == null) return false;
        if (exception instanceof IllegalStateException) return true;
        if (exception.getCause() != null) return isAccountStatusException(exception.getCause());
        return false;
    }
}
