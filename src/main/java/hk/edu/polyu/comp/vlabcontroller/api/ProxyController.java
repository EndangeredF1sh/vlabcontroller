package hk.edu.polyu.comp.vlabcontroller.api;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.API.APIResponseBody;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
public class ProxyController extends BaseController {
    private final Logger log = LogManager.getLogger(ProxyController.class);

    @GetMapping(value = "/api/proxyspec", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponseBody<List<ProxySpec>>> listProxySpecs() {
        return APIResponseBody.success(proxyService.getProxySpecs(null, false));
    }

    @GetMapping(value = "/api/proxyspec/{proxySpecId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponseBody<ProxySpec>> getProxySpec(@PathVariable String proxySpecId) {
        ProxySpec spec = proxyService.findProxySpec(s -> s.getId().equals(proxySpecId), false);
        if (spec == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return APIResponseBody.success(spec);
    }

    @GetMapping(value = "/api/proxy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponseBody<List<Proxy>>> listProxies() {
        return APIResponseBody.success(proxyService.getProxies(null, false));
    }

    @GetMapping(value = "/api/proxy/{proxyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponseBody<Proxy>> getProxy(@PathVariable String proxyId) {
        Proxy proxy = proxyService.findProxy(p -> p.getId().equals(proxyId), false);
        if (proxy == null) return APIResponseBody.resourceNotFound();
        return APIResponseBody.success(proxy);
    }

    @PostMapping(value = "/api/proxy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponseBody<Proxy>> startProxy(@RequestParam String proxySpecId) {
        ProxySpec baseSpec = proxyService.findProxySpec(s -> s.getId().equals(proxySpecId), false);
        if (baseSpec == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Proxy proxy = findUserProxy(proxySpecId);
        if (proxy != null) {
            return APIResponseBody.success(proxy);
        }

        if (!userService.isAdmin()) {
            int containerLimit = environment.getProperty("proxy.container-quantity-limit", Integer.class, 2);
            int proxyCount = proxyService.getProxies(p -> p.getUserId().equals(userService.getCurrentUserId()) && !p.getSpec().getId().equals("filebrowser"), false).size();
            if (proxyCount >= containerLimit) {
                return APIResponseBody.failed("container limit exceeded");
            }
        }

        ProxySpec spec = proxyService.resolveProxySpec(baseSpec, null, null);
        proxy = proxyService.startProxy(spec, false);
        return APIResponseBody.success(proxy);
    }

    @PostMapping(value = "/api/proxy/{proxyId}/extension", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponseBody<Proxy>> extendExpiration(@PathVariable String proxyId, @RequestParam long extendMs) {
        Proxy proxy = proxyService.findProxy(p -> p.getId().equals(proxyId), false);
        if (proxy == null) return APIResponseBody.resourceNotFound();

        if (extendMs < 0) return APIResponseBody.badRequest("extendMs should be a positive integer");

        if (extendMs > Duration.ofHours(12).toMillis()) return APIResponseBody.badRequest("extendMs should be less than 12 hours");
        long expiration = proxy.getExpirationTimestamp();
        proxy.setExpirationTimestamp(expiration + extendMs);

        return APIResponseBody.success(proxy);
    }

//    @PostMapping(value = "/api/proxy", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<Proxy> startProxy(@RequestBody ProxySpec proxySpec) {
//        ProxySpec spec = proxyService.resolveProxySpec(null, proxySpec, null);
//        Proxy proxy = proxyService.startProxy(spec, false);
//        return new ResponseEntity<>(proxy, HttpStatus.CREATED);
//    }

    @DeleteMapping(value = "/api/proxy/{proxyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponseBody<Proxy>> stopProxy(@PathVariable String proxyId) {
        Proxy proxy = proxyService.findProxy(p -> p.getId().equals(proxyId), false);
        if (proxy == null) return APIResponseBody.resourceNotFound();

        proxyService.stopProxy(proxy, true, false, 0);
        return APIResponseBody.success();
    }
}
