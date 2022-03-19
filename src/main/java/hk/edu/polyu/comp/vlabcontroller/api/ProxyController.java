package hk.edu.polyu.comp.vlabcontroller.api;

import com.google.common.collect.Sets;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import io.vavr.Function1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class ProxyController extends BaseController {
    private final ProxyService proxyService;

    @GetMapping(value = "/api/proxyspec", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProxySpec> listProxySpecs() {
        return proxyService.getProxySpecs(null, false);
    }

    @GetMapping(value = "/api/proxyspec/{proxySpecId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProxySpec> getProxySpec(@PathVariable String proxySpecId) {
        return findProxySpecByIdAndACL(proxySpecId)
            .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/api/proxy", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Proxy> listProxies() {
        return proxyService.getProxies(null, false);
    }

    @GetMapping(value = "/api/proxy/{proxyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Proxy> getProxy(@PathVariable String proxyId) {
        return findProxyByIdAndACL(proxyId, false)
            .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/api/proxy/{proxyId}/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> setMetadata(
        @PathVariable String proxyId, @RequestBody Map<String, Object> payload,
        @RequestParam(required = false, defaultValue = "true") boolean override
    ) {
        return findProxyByIdAndACL(proxyId, true)
            .map((Function1<Proxy, ResponseEntity<Map<String, Object>>>) proxy -> {
                var metadata = proxy.getMetadata();
                var duplicates = Sets.intersection(metadata.keySet(), payload.keySet());
                var shouldPut = duplicates.isEmpty() || override;
                if (shouldPut) metadata.putAll(payload);
                return shouldPut ? ResponseEntity.ok(metadata) : ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("conflicts", duplicates));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/api/proxy/{proxyId}/metadata/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> setMetadata(
        @PathVariable String proxyId, @PathVariable String key, @RequestBody Object value,
        @RequestParam(required = false, defaultValue = "true") boolean override
    ) {
        return findProxyByIdAndACL(proxyId, true)
            .map((Function1<Proxy, ResponseEntity<Map<String, Object>>>) proxy -> {
                var metadata = proxy.getMetadata();
                var shouldPut = !metadata.containsKey(key) || override;
                if (shouldPut) metadata.put(key, value);
                return shouldPut ? ResponseEntity.ok(metadata) : ResponseEntity.status(HttpStatus.CONFLICT).build();
            }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/api/proxy/{proxyId}/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getMetadata(@PathVariable String proxyId) {
        return findProxyByIdAndACL(proxyId, true)
            .flatMap(proxy -> Optional.ofNullable(proxy.getMetadata()).map(ResponseEntity::ok))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/api/proxy/{proxyId}/metadata/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getMetadata(@PathVariable String proxyId, @PathVariable String key) {
        return findProxyByIdAndACL(proxyId, true)
            .flatMap(proxy -> Optional.ofNullable(proxy.getMetadata().get(key)).map(ResponseEntity::ok))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/api/proxy/{proxyId}/metadata/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteMetadata(
        @PathVariable String proxyId, @PathVariable String key,
        @RequestParam(required = false, defaultValue = "true") boolean silentIfNotExist
    ) {
        return findProxyByIdAndACL(proxyId, true)
            .map((Function1<Proxy, ResponseEntity<Void>>) proxy -> {
                var metadata = proxy.getMetadata();
                var shouldRemove = metadata.containsKey(key) || silentIfNotExist;
                if (shouldRemove) metadata.remove(key);
                return shouldRemove ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/api/proxy/{proxySpecId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Proxy> startProxy(@PathVariable String proxySpecId, @RequestBody(required = false) Set<RuntimeSetting> runtimeSettings) {
        return findProxySpecByIdAndACL(proxySpecId)
            .map(baseSpec -> {
                var spec = proxyService.resolveProxySpec(baseSpec, null, runtimeSettings);
                var proxy = proxyService.startProxy(spec, false);
                return ResponseEntity.status(HttpStatus.CREATED).body(proxy);
            }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/api/proxy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Proxy> startProxy(@RequestBody ProxySpec proxySpec) {
        var spec = proxyService.resolveProxySpec(null, proxySpec, null);
        var proxy = proxyService.startProxy(spec, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(proxy);
    }

    @DeleteMapping(value = "/api/proxy/{proxyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> stopProxy(@PathVariable String proxyId) {
        return findProxyByIdAndACL(proxyId, false)
            .map(proxy -> {
                proxyService.stopProxy(proxy, true, false, Duration.ZERO);
                return ResponseEntity.ok("Proxy stopped");
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private Optional<Proxy> findProxyByIdAndACL(String proxyId, boolean ignoreAccessControl) {
        return Optional.ofNullable(proxyService.findProxy(p -> p.getId().equals(proxyId), ignoreAccessControl));
    }

    private Optional<ProxySpec> findProxySpecByIdAndACL(String specId) {
        return Optional.ofNullable(proxyService.findProxySpec(p -> p.getId().equals(specId), false));
    }
}
