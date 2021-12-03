package hk.edu.polyu.comp.vlabcontroller.auth.impl.keycloak;

import org.keycloak.adapters.OIDCAuthenticationError;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationFailureHandler;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationFaillureHandler extends KeycloakAuthenticationFailureHandler {
    public static final String SP_KEYCLOAK_ERROR_REASON = "SP_KEYCLOAK_ERROR_REASON";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // Note: Keycloak calls sendError before this method gets called, therefore we cannot do much with reuqest.
        // We now set a flag in the session indicating the reason of the Keycloak error.
        // The error page can then properly handle this.

        Object obj = request.getAttribute("org.keycloak.adapters.spi.AuthenticationError");
        if (obj instanceof org.keycloak.adapters.OIDCAuthenticationError) {
            OIDCAuthenticationError authError = (OIDCAuthenticationError) obj;
            request.getSession().setAttribute(SP_KEYCLOAK_ERROR_REASON, authError.getReason());
        }
    }
}
