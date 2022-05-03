package hk.edu.polyu.comp.vlabcontroller.auth.impl;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.config.ServerProperties;
import hk.edu.polyu.comp.vlabcontroller.security.FixedDefaultOAuth2AuthorizationRequestResolver;
import hk.edu.polyu.comp.vlabcontroller.util.SessionHelper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.AuthorizedUrl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RefreshScope
public class OpenIDAuthenticationBackend implements IAuthenticationBackend {

    public static final String NAME = "openid";

    private static final String REG_ID = "vlab";
    private static final String ENV_TOKEN_NAME = "VLAB_OIDC_ACCESS_TOKEN";

    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @Setter(onMethod_ = {@Inject})
    private ProxyProperties proxyProperties;

    @Setter(onMethod_ = {@Inject})
    private ServerProperties serverProperties;

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
        var clientRegistrationRepo = createClientRepo();
        oAuth2AuthorizedClientRepository = new HttpSessionOAuth2AuthorizedClientRepository();

        anyRequestConfigurer.authenticated();

        http
            .oauth2Login()
            .loginPage("/login")
            .clientRegistrationRepository(clientRegistrationRepo)
            .authorizedClientRepository(oAuth2AuthorizedClientRepository)
            .authorizationEndpoint()
            .authorizationRequestResolver(new FixedDefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepo, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI))
            .and()
            .failureHandler((request, response, exception) -> {
                log.error("an error occured: {}", exception);
                response.sendRedirect(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth-error").build().toUriString());
            })
            .userInfoEndpoint()
            .userAuthoritiesMapper(createAuthoritiesMapper())
            .oidcUserService(createOidcUserService());
    }

    @Override
    public void configureAuthenticationManagerBuilder(AuthenticationManagerBuilder auth) throws Exception {
        // Nothing to do.
    }

    public String getLoginRedirectURI() {
        return SessionHelper.getContextPath(serverProperties, false)
            + OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
            + "/" + REG_ID;
    }

    @Override
    public String getLogoutSuccessURL() {
        var logoutURL = proxyProperties.getOpenID().getLogoutUrl();
        if (logoutURL == null || logoutURL.isBlank())
            logoutURL = IAuthenticationBackend.super.getLogoutSuccessURL();
        return logoutURL;
    }

    @Override
    public void customizeContainerEnv(Map<String, String> env) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;

        var user = (OidcUser) auth.getPrincipal();
        var request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        var client = oAuth2AuthorizedClientRepository.loadAuthorizedClient(REG_ID, auth, request);
        if (client == null || client.getAccessToken() == null) return;

        env.put(ENV_TOKEN_NAME, client.getAccessToken().getTokenValue());
    }

    protected ClientRegistrationRepository createClientRepo() {
        var openID = proxyProperties.getOpenID();
        return new InMemoryClientRegistrationRepository(
            ClientRegistration.withRegistrationId(REG_ID)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientName(REG_ID)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(Stream.concat(Stream.of("openid", "email"), openID.getScopes().stream()).collect(Collectors.toSet()))
                .userNameAttributeName(openID.getUsernameAttribute())
                .authorizationUri(openID.getAuthUrl())
                .tokenUri(openID.getTokenUrl())
                .jwkSetUri(openID.getJwksUrl())
                .clientId(openID.getClientId())
                .clientSecret(openID.getClientSecret())
                .build()
        );
    }

    protected GrantedAuthoritiesMapper createAuthoritiesMapper() {
        var rolesClaimName = proxyProperties.getOpenID().getRolesClaim();
        if (rolesClaimName != null && !rolesClaimName.isEmpty()) {
            return authorities -> authorities.stream()
                .filter(OidcUserAuthority.class::isInstance)
                .map(OidcUserAuthority.class::cast)
                .map(OidcUserAuthority::getIdToken)
                .flatMap(idToken -> {
                    var claims = idToken.getClaims();
                    if (log.isDebugEnabled()) {
                        var lineSep = System.getProperty("line.separator");
                        var claims_ = claims.entrySet().stream()
                            .map(e -> String.format("%s -> %s", e.getKey(), e.getValue()))
                            .collect(Collectors.joining(lineSep));
                        log.debug(String.format("Checking for roles in claim '%s'. Available claims in ID token (%d):%s%s",
                            rolesClaimName, claims.size(), lineSep, claims_));
                    }

                    var claimValue = claims.get(rolesClaimName);
                    if (claimValue == null) {
                        log.debug("No matching claim found.");
                    } else {
                        log.debug(String.format("Matching claim found: %s -> %s (%s)", rolesClaimName, claimValue, claimValue.getClass()));
                    }

                    // Workaround: in some cases, getClaimAsStringList fails to parse??
                    return Optional.ofNullable(idToken.getClaimAsStringList(rolesClaimName))
                        .map(Collection::stream)
                        .orElseGet(() -> {
                            try {
                                var value = new JSONParser(JSONParser.MODE_PERMISSIVE).parse((String) claimValue);
                                if (value instanceof List) {
                                    return ((List<?>) value).stream().map(Object::toString);
                                }
                            } catch (ParseException e) {
                                // Unable to parse JSON
                            }
                            if (log.isDebugEnabled())
                                log.debug("Failed to parse claim value as an array: " + claimValue);
                            return Stream.empty();
                        })
                        .map(role -> role.toUpperCase().startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(String::toUpperCase)
                        .map(SimpleGrantedAuthority::new);
                })
                .collect(Collectors.toSet());
        }
        return authorities -> authorities;
    }

    protected OidcUserService createOidcUserService() {
        // Use a custom UserService that supports the 'emails' array attribute.
        return new OidcUserService() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
                OidcUser user;
                try {
                    user = super.loadUser(userRequest);
                } catch (IllegalArgumentException ex) {
                    throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST), "Error while loading user info", ex);
                }
                var nameAttributeKey = proxyProperties.getOpenID().getUsernameAttribute();
                return new CustomNameOidcUser(new HashSet<>(user.getAuthorities()), user.getIdToken(), user.getUserInfo(), nameAttributeKey);
            }
        };
    }

    private static class CustomNameOidcUser extends DefaultOidcUser {

        private static final long serialVersionUID = 7563253562760236634L;
        private static final String ID_ATTR_EMAILS = "emails";

        private final boolean isEmailsAttribute;

        public CustomNameOidcUser(Set<GrantedAuthority> authorities, OidcIdToken idToken, OidcUserInfo userInfo, String nameAttributeKey) {
            super(authorities, idToken, userInfo, nameAttributeKey);
            this.isEmailsAttribute = nameAttributeKey.equals(ID_ATTR_EMAILS);
        }

        @Override
        public String getName() {
            if (isEmailsAttribute) {
                var emails = getAttributes().get(ID_ATTR_EMAILS);
                if (emails instanceof String[]) return ((String[]) emails)[0];
                else if (emails instanceof JSONArray) return ((JSONArray) emails).get(0).toString();
                else return emails.toString();
            } else return super.getName();
        }
    }
}
