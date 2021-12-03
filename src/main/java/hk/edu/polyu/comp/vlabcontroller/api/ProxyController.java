package hk.edu.polyu.comp.vlabcontroller.api;

import hk.edu.polyu.comp.vlabcontroller.model.runtime.Proxy;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.RuntimeSetting;
import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import hk.edu.polyu.comp.vlabcontroller.service.ProxyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
public class ProxyController extends BaseController {
    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping(value = "/api/proxyspec", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProxySpec> listProxySpecs() {
        return proxyService.getProxySpecs(null, false);
    }

    @GetMapping(value = "/api/proxyspec/{proxySpecId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProxySpec> getProxySpec(@PathVariable String proxySpecId) {
        ProxySpec spec = proxyService.findProxySpec(s -> s.getId().equals(proxySpecId), false);
        if (spec == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(spec, HttpStatus.OK);
    }

    @GetMapping(value = "/api/proxy", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Proxy> listProxies() {
        return proxyService.getProxies(null, false);
    }

    @GetMapping(value = "/api/proxy/{proxyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Proxy> getProxy(@PathVariable String proxyId) {
        Proxy proxy = proxyService.findProxy(p -> p.getId().equals(proxyId), false);
        if (proxy == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(proxy, HttpStatus.OK);
    }

    @PostMapping(value = "/api/proxy/{proxySpecId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Proxy> startProxy(@PathVariable String proxySpecId, @RequestBody(required = false) Set<RuntimeSetting> runtimeSettings) {
        ProxySpec baseSpec = proxyService.findProxySpec(s -> s.getId().equals(proxySpecId), false);
        if (baseSpec == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        ProxySpec spec = proxyService.resolveProxySpec(baseSpec, null, runtimeSettings);
        Proxy proxy = proxyService.startProxy(spec, false);
        return new ResponseEntity<>(proxy, HttpStatus.CREATED);
    }

    @PostMapping(value = "/api/proxy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Proxy> startProxy(@RequestBody ProxySpec proxySpec) {
        ProxySpec spec = proxyService.resolveProxySpec(null, proxySpec, null);
        Proxy proxy = proxyService.startProxy(spec, false);
        return new ResponseEntity<>(proxy, HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/api/proxy/{proxyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> stopProxy(@PathVariable String proxyId) {
        Proxy proxy = proxyService.findProxy(p -> p.getId().equals(proxyId), false);
        if (proxy == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        proxyService.stopProxy(proxy, true, false);
        return new ResponseEntity<>("Proxy stopped", HttpStatus.OK);
    }
}
