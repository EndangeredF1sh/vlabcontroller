package hk.edu.polyu.comp.vlabcontroller.auth.impl;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.Setter;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.rcp.RemoteAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.AuthorizedUrl;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.List;

/**
 * Web service authentication method where user/password combinations are
 * checked by a HTTP call to a remote web service.
 */
@RefreshScope
public class WebServiceAuthenticationBackend implements IAuthenticationBackend {

    public static final String NAME = "webservice";

    @Setter(onMethod_ = {@Inject})
    private ProxyProperties proxyProperties;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean hasAuthorization() {
        return true;
    }

    @Override
    public void configureHttpSecurity(HttpSecurity http, AuthorizedUrl anyRequestConfigurer) throws Exception {
        // Nothing to do.
    }

    @Override
    public void configureAuthenticationManagerBuilder(AuthenticationManagerBuilder auth) throws Exception {
        var authenticationProvider = new RemoteAuthenticationProvider();
        authenticationProvider.setRemoteAuthenticationManager((username, password) -> {
            var restTemplate = new RestTemplate();

            var headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            try {
                var body = String.format(proxyProperties.getWebService().getAuthenticationRequestBody(), username, password);
                var loginUrl = proxyProperties.getWebService().getAuthenticationUrl();
                var result = restTemplate.exchange(loginUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
                if (result.getStatusCode() == HttpStatus.OK) {
                    return List.of();
                }
                throw new AuthenticationServiceException("Unknown response received " + result);
            } catch (HttpClientErrorException e) {
                throw new BadCredentialsException("Invalid username or password");
            } catch (RestClientException e) {
                throw new AuthenticationServiceException("Internal error " + e.getMessage());
            }

        });
        auth.authenticationProvider(authenticationProvider);
    }

}
