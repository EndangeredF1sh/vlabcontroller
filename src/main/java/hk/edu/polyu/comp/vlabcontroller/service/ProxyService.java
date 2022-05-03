package hk.edu.polyu.comp.vlabcontroller.service;

import hk.edu.polyu.comp.vlabcontroller.VLabControllerException;
import hk.edu.polyu.comp.vlabcontroller.backend.IContainerBackend;
import hk.edu.polyu.comp.vlabcontroller.event.ProxyStartEvent;
import hk.edu.polyu.comp.vlabcontroller.event.ProxyStartFailedEvent;
import hk.edu.polyu.comp.vlabcontroller.event.ProxyStopEvent;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.ProxyStatus;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.spec.IProxySpecMergeStrategy;
import hk.edu.polyu.comp.vlabcontroller.spec.IProxySpecProvider;
import hk.edu.polyu.comp.vlabcontroller.spec.ProxySpecException;
import hk.edu.polyu.comp.vlabcontroller.util.ProxyMappingManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * This service is the entry point for working with proxies.
 * It offers methods to list, start and stop proxies, as well
 * as methods for managing proxy specs.
 * </p><p>
 * A note about security: these methods are considered internal API,
 * and are therefore allowed to bypass security checks.<br/>
 * The caller is always responsible for performing security
 * checks before manipulating proxies.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class ProxyService {
    private final List<Proxy> activeProxies = Collections.synchronizedList(new ArrayList<>());
    private final ThreadPoolTaskScheduler taskScheduler;

    private final IProxySpecProvider baseSpecProvider;
    private final IProxySpecMergeStrategy specMergeStrategy;
    private final IContainerBackend backend;
    private final ProxyMappingManager mappingManager;
    private final UserService userService;
    private final LogService logService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private List<Future<?>> containerKillerFutures = new ArrayList<>();

    @PreDestroy
    public void shutdown() {
        containerKillerFutures.forEach(x -> x.cancel(true));

        for (var proxy : activeProxies) {
            try {
                backend.stopProxy(proxy);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Find the ProxySpec that matches the given ID.
     *
     * @param id The ID to look for.
     * @return A matching ProxySpec, or null if no match was found.
     */
    public ProxySpec getProxySpec(String id) {
        if (id == null || id.isEmpty()) return null;
        return findProxySpec(spec -> spec.getId().equals(id), true);
    }

    /**
     * Find the first ProxySpec that matches the given filter.
     *
     * @param filter              The filter to match, may be null.
     * @param ignoreAccessControl True to search in all ProxySpecs, regardless of the current security context.
     * @return The first ProxySpec found that matches the filter, or null if no match was found.
     */
    public ProxySpec findProxySpec(Predicate<ProxySpec> filter, boolean ignoreAccessControl) {
        return getProxySpecs(filter, ignoreAccessControl).stream().findAny().orElse(null);
    }

    /**
     * Find all ProxySpecs that match the given filter.
     *
     * @param filter              The filter to match, or null.
     * @param ignoreAccessControl True to search in all ProxySpecs, regardless of the current security context.
     * @return A List of matching ProxySpecs, may be empty.
     */
    public List<ProxySpec> getProxySpecs(Predicate<ProxySpec> filter, boolean ignoreAccessControl) {
        return baseSpecProvider.getSpecs().stream()
            .filter(spec -> ignoreAccessControl || userService.canAccess(spec))
            .filter(spec -> filter == null || filter.test(spec))
            .collect(Collectors.toList());
    }

    /**
     * Resolve a ProxySpec. A base spec will be merged with a runtime spec (one of them is optional),
     * and an optional set of runtime settings will be applied to the resulting spec.
     *
     * @param baseSpec        The base spec, provided by the configured {@link IProxySpecProvider}.
     * @param runtimeSpec     The runtime spec, may be null if <b>baseSpec</b> is not null.
     * @param runtimeSettings Optional runtime settings.
     * @return A merged ProxySpec that can be used to launch new proxies.
     * @throws ProxySpecException If the merge fails for any reason.
     * @see IProxySpecMergeStrategy
     */
    public ProxySpec resolveProxySpec(ProxySpec baseSpec, ProxySpec runtimeSpec, Set<RuntimeSetting> runtimeSettings) throws ProxySpecException {
        return specMergeStrategy.merge(baseSpec, runtimeSpec, runtimeSettings);
    }

    /**
     * Find a proxy using its ID.
     *
     * @param id The ID of the proxy to find.
     * @return The matching proxy, or null if no match was found.
     */
    public Proxy getProxy(String id) {
        return findProxy(proxy -> proxy.getId().equals(id), true);
    }

    /**
     * Find The first proxy that matches the given filter.
     *
     * @param filter              The filter to apply while searching, or null.
     * @param ignoreAccessControl True to search in all proxies, regardless of the current security context.
     * @return The first proxy found that matches the filter, or null if no match was found.
     */
    public Proxy findProxy(Predicate<Proxy> filter, boolean ignoreAccessControl) {
        return getProxies(filter, ignoreAccessControl).stream().findAny().orElse(null);
    }

    /**
     * Find all proxies that match an optional filter.
     *
     * @param filter              The filter to match, or null.
     * @param ignoreAccessControl True to search in all proxies, regardless of the current security context.
     * @return A List of matching proxies, may be empty.
     */
    public List<Proxy> getProxies(Predicate<Proxy> filter, boolean ignoreAccessControl) {
        var isAdmin = userService.isAdmin();
        List<Proxy> matches = new ArrayList<>();
        synchronized (activeProxies) {
            for (var proxy : activeProxies) {
                var hasAccess = ignoreAccessControl || isAdmin || userService.isOwner(proxy);
                if (hasAccess && (filter == null || filter.test(proxy))) matches.add(proxy);
            }
        }
        return matches;
    }

    /**
     * Launch a new proxy using the given ProxySpec.
     *
     * @param spec                The ProxySpec to base the new proxy on.
     * @param ignoreAccessControl True to allow access to the given ProxySpec, regardless of the current security context.
     * @return The newly launched proxy.
     * @throws VLabControllerException If the proxy fails to start for any reason.
     */
    public Proxy startProxy(ProxySpec spec, boolean ignoreAccessControl) throws VLabControllerException {
        if (!ignoreAccessControl && !userService.canAccess(spec)) {
            throw new AccessDeniedException(String.format("Cannot start proxy %s: access denied", spec.getId()));
        }

        var proxy = Proxy.builder()
            .status(ProxyStatus.New)
            .userId(userService.getCurrentUserId())
            .spec(spec.copy())
            .admin(userService.isAdmin())
            .build();
        activeProxies.add(proxy);

        try {
            backend.startProxy(proxy);
        } finally {
            if (proxy.getStatus() != ProxyStatus.Up) {
                activeProxies.remove(proxy);
                var event = ProxyStartFailedEvent.builder().source(this).specId(spec.getId()).userId(proxy.getUserId()).build();
                applicationEventPublisher.publishEvent(event);
            }
        }

        for (var target : proxy.getTargets().entrySet()) {
            mappingManager.addMapping(proxy.getId(), target.getKey(), target.getValue());
        }

        if (logService.isLoggingEnabled()) {
            var outputAttacher = backend.getOutputAttacher(proxy);
            if (outputAttacher == null) {
                log.warn("Cannot log proxy output: " + backend.getClass() + " does not support output attaching.");
            } else {
                logService.attachToOutput(proxy, outputAttacher);
            }
        }

        log.info(String.format("Proxy activated [user: %s] [spec: %s] [id: %s]", proxy.getUserId(), spec.getId(), proxy.getId()));
        var event = ProxyStartEvent.builder()
            .source(this).proxyId(proxy.getId()).specId(spec.getId()).userId(proxy.getUserId())
            .startupTime(proxy.getStartupTimestamp().minus(proxy.getCreatedTimestamp()))
            .build();
        applicationEventPublisher.publishEvent(event);

        return proxy;
    }

    /**
     * Stop a running proxy
     *
     * @param proxy               The proxy to stop.
     * @param async               True to return immediately and stop the proxy in an asynchronous manner.
     * @param ignoreAccessControl True to allow access to any proxy, regardless of the current security context.
     * @param silenceOffset       Milliseconds to subtract idle silence period, report accurate usage time.
     */
    public void stopProxy(Proxy proxy, boolean async, boolean ignoreAccessControl, Duration silenceOffset) {
        if (!ignoreAccessControl && !userService.isAdmin() && !userService.isOwner(proxy)) {
            throw new AccessDeniedException(String.format("Cannot stop proxy %s: access denied", proxy.getId()));
        }

        activeProxies.remove(proxy);

        Runnable releaser = () -> {
            try {
                backend.stopProxy(proxy);
                logService.detach(proxy);
                log.info(String.format("Proxy released [user: %s] [spec: %s] [id: %s]", proxy.getUserId(), proxy.getSpec().getId(), proxy.getId()));
                if (DurationUtils.isPositive(proxy.getStartupTimestamp())) {
                    var event = ProxyStopEvent.builder()
                        .usageTime(Duration.ofMillis(System.currentTimeMillis()).minus(proxy.getStartupTimestamp()).minus(silenceOffset))
                        .source(this).proxyId(proxy.getId()).userId(proxy.getUserId()).specId(proxy.getSpec().getId())
                        .build();
                    applicationEventPublisher.publishEvent(event);
                }
            } catch (Exception e) {
                log.error("Failed to release proxy " + proxy.getId(), e);
            }
        };
        if (async) containerKillerFutures.add(taskScheduler.submit(releaser));
        else releaser.run();

        mappingManager.removeProxyMapping(proxy.getId());
    }
}
