package eu.openanalytics.containerproxy.security;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Disables the handling of OAuth on the `/app_direct` URL.
 * See issue #23799.
 * <p>
 * Without this, the Filter will eat the body of the (POST) request. As a result Undertow will not be able
 * to proxy the request to the container.
 */
public class FixedDefaultOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public FixedDefaultOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, String authorizationRequestBaseUri) {
        delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        if (request.getServletPath().startsWith("/app_direct")) {
            return null;
        }
        return delegate.resolve(request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        if (request.getServletPath().startsWith("/app_direct")) {
            return null;
        }
        return delegate.resolve(request, clientRegistrationId);
    }

}
