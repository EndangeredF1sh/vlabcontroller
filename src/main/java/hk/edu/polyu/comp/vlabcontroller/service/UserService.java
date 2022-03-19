package hk.edu.polyu.comp.vlabcontroller.service;

import hk.edu.polyu.comp.vlabcontroller.auth.IAuthenticationBackend;
import hk.edu.polyu.comp.vlabcontroller.backend.strategy.IProxyLogoutStrategy;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.event.AuthFailedEvent;
import hk.edu.polyu.comp.vlabcontroller.event.UserLoginEvent;
import hk.edu.polyu.comp.vlabcontroller.event.UserLogoutEvent;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.util.SessionHelper;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
@RefreshScope
public class UserService {

    private final Map<String, String> userInitiatedLogoutMap = new HashMap<>();
    private final ProxyProperties proxyProperties;
    private final IAuthenticationBackend authBackend;
    private final IProxyLogoutStrategy logoutStrategy;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Authentication getCurrentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public String getCurrentUserId() {
        return getUserId(getCurrentAuth());
    }

    public Collection<String> getAdminGroups() {
        return proxyProperties.getAdminGroups().stream()
            .filter(Predicate.not(String::isBlank))
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
    }

    public Collection<String> getGroups() {
        return getGroups(getCurrentAuth());
    }

    public Collection<String> getGroups(Authentication auth) {
        return auth.getAuthorities().stream().map(grantedAuth -> {
            var authName = grantedAuth.getAuthority().toUpperCase();
            if (authName.startsWith("ROLE_")) authName = authName.substring(5);
            return authName;
        }).collect(Collectors.toList());
    }

    public boolean isAdmin() {
        return isAdmin(getCurrentAuth());
    }

    public boolean isAdmin(Authentication auth) {
        return getAdminGroups().stream().anyMatch(adminGroup -> isMember(auth, adminGroup));
    }

    public boolean canAccess(ProxySpec spec) {
        return canAccess(getCurrentAuth(), spec);
    }

    public boolean canAccess(Authentication auth, ProxySpec spec) {
        if (auth == null || spec == null) return false;
        if (auth instanceof AnonymousAuthenticationToken) return !authBackend.hasAuthorization();
        var groups = spec.getAccessGroups();
        return groups.isEmpty() || groups.stream().anyMatch(group -> isMember(auth, group));
    }

    public boolean isOwner(Proxy proxy) {
        return isOwner(getCurrentAuth(), proxy);
    }

    public boolean isOwner(Authentication auth, Proxy proxy) {
        if (auth == null || proxy == null) return false;
        return proxy.getUserId().equals(getUserId(auth));
    }

    private boolean isMember(Authentication auth, String groupName) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken || groupName == null) return false;
        return getGroups(auth).stream().anyMatch(group -> group.equalsIgnoreCase(groupName));
    }

    private String getUserId(Authentication auth) {
        if (auth == null) return null;
        if (auth instanceof AnonymousAuthenticationToken) {
            // Anonymous authentication: use the session id instead of the user name.
            return SessionHelper.getCurrentSessionId(true);
        }
        return auth.getName();
    }

    @EventListener
    public void onAbstractAuthenticationFailureEvent(AbstractAuthenticationFailureEvent event) {
        var source = event.getAuthentication();
        Exception e = event.getException();
        log.info(String.format("Authentication failure [user: %s] [error: %s]", source.getName(), e.getMessage()));
        var userId = getUserId(source);

        applicationEventPublisher.publishEvent(new AuthFailedEvent(
                this,
                userId,
                RequestContextHolder.currentRequestAttributes().getSessionId()));
    }

    public void logout(Authentication auth) {
        var userId = getUserId(auth);
        if (userId == null) return;

        if (logoutStrategy != null) logoutStrategy.onLogout(userId, false);
        log.info(String.format("User logged out [user: %s]", userId));

        var session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        var sessionId = session.getId();
        userInitiatedLogoutMap.put(sessionId, "true");
        applicationEventPublisher.publishEvent(new UserLogoutEvent(
                this,
                userId,
                sessionId,
                false));
    }

    @EventListener
    public void onAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
        var auth = event.getAuthentication();
        var userName = auth.getName();

        var session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
        var firstLogin = session.getAttribute("firstLogin") == null || (Boolean) session.getAttribute("firstLogin");
        if (firstLogin) {
            session.setAttribute("firstLogin", false);
        } else {
            return;
        }

        log.info(String.format("User logged in [user: %s]", userName));

        var userId = getUserId(auth);
        applicationEventPublisher.publishEvent(UserLoginEvent.builder().source(this).userId(userId).sessionId(RequestContextHolder.currentRequestAttributes().getSessionId()).build());
    }

    @EventListener
    public void onHttpSessionDestroyedEvent(HttpSessionDestroyedEvent event) {
		/*
			ref: https://docs.spring.io/spring-session/docs/current/api/org/springframework/session/events/AbstractSessionEvent.html
			For some SessionRepository implementations it may not be possible to get the original session in which case this may be null.
			event.getSession() != ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession() in Redis Session Repository
			Session Attributes set in logout() cannot be fetched here
			but these two session instances have same sessionId, an additional Map can be used as workaround
		 */
        var userInitiatedLogout = userInitiatedLogoutMap.remove(event.getId());
        if (userInitiatedLogout != null && userInitiatedLogout.equals("true")) {
            // user initiated the logout
            // event already handled by the logout() function above -> ignore it
        } else {
            // user did not initiated the logout -> session expired
            // not already handled by any other handler
            var eventBuilder = UserLogoutEvent.builder().source(this);

            var sid = Option.<String>none();
            var uid = Option.<String>none();

            if (!event.getSecurityContexts().isEmpty()) {
                var securityContext = event.getSecurityContexts().get(0);
                if (securityContext == null) return;

                var userId = securityContext.getAuthentication().getName();
                logoutStrategy.onLogout(userId, true);
                log.info(String.format("HTTP session expired [user: %s]", userId));
                uid = Option.some(userId);
                sid = Option.some(RequestContextHolder.currentRequestAttributes().getSessionId());
            } else if (authBackend.getName().equals("none")) {
                var id = event.getSession().getId();
                log.info(String.format("Anonymous user logged out [user: %s]", id));
                sid = uid = Option.some(id);
            }
            applicationEventPublisher.publishEvent(eventBuilder.userId(uid.get()).sessionId(sid.get()).wasExpired(true).build());
        }
    }

    @EventListener
    public void onHttpSessionCreated(HttpSessionCreatedEvent event) {
        if (authBackend.getName().equals("none")) {
            var id = event.getSession().getId();
            log.info(String.format("Anonymous user logged in [user: %s]", id));
            applicationEventPublisher.publishEvent(UserLoginEvent.builder().source(this).userId(id).sessionId(id).build());
        }
    }

}
