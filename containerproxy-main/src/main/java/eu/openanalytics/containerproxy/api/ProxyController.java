package eu.openanalytics.containerproxy.api;

import eu.openanalytics.containerproxy.model.runtime.Proxy;
import eu.openanalytics.containerproxy.model.runtime.RuntimeSetting;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.service.ProxyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;


@RestController
public class ProxyController extends BaseController {
  
  @Inject
  private ProxyService proxyService;
  
  @RequestMapping(value = "/api/proxyspec", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ProxySpec> listProxySpecs() {
    return proxyService.getProxySpecs(null, false);
  }
  
  @RequestMapping(value = "/api/proxyspec/{proxySpecId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ProxySpec> getProxySpec(@PathVariable String proxySpecId) {
    ProxySpec spec = proxyService.findProxySpec(s -> s.getId().equals(proxySpecId), false);
    if (spec == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    return new ResponseEntity<>(spec, HttpStatus.OK);
  }
  
  @RequestMapping(value = "/api/proxy", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public List<Proxy> listProxies() {
    return proxyService.getProxies(null, false);
  }
  
  @RequestMapping(value = "/api/proxy/{proxyId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Proxy> getProxy(@PathVariable String proxyId) {
    Proxy proxy = proxyService.findProxy(p -> p.getId().equals(proxyId), false);
    if (proxy == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    return new ResponseEntity<>(proxy, HttpStatus.OK);
  }
  
  @RequestMapping(value = "/api/proxy/{proxySpecId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Proxy> startProxy(@PathVariable String proxySpecId, @RequestBody(required = false) Set<RuntimeSetting> runtimeSettings) {
    ProxySpec baseSpec = proxyService.findProxySpec(s -> s.getId().equals(proxySpecId), false);
    if (baseSpec == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    
    ProxySpec spec = proxyService.resolveProxySpec(baseSpec, null, runtimeSettings);
    Proxy proxy = proxyService.startProxy(spec, false);
    return new ResponseEntity<>(proxy, HttpStatus.CREATED);
  }
  
  @RequestMapping(value = "/api/proxy", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Proxy> startProxy(@RequestBody ProxySpec proxySpec) {
    ProxySpec spec = proxyService.resolveProxySpec(null, proxySpec, null);
    Proxy proxy = proxyService.startProxy(spec, false);
    return new ResponseEntity<>(proxy, HttpStatus.CREATED);
  }
  
  @RequestMapping(value = "/api/proxy/{proxyId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> stopProxy(@PathVariable String proxyId) {
    Proxy proxy = proxyService.findProxy(p -> p.getId().equals(proxyId), false);
    if (proxy == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    
    proxyService.stopProxy(proxy, true, false);
    return new ResponseEntity<>("Proxy stopped", HttpStatus.OK);
  }
}
