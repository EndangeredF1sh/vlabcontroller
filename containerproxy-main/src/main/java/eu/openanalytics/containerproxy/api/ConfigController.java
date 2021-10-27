package eu.openanalytics.containerproxy.api;

import eu.openanalytics.containerproxy.event.ConfigUpdateEvent;
import eu.openanalytics.containerproxy.util.ConfigFileHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;

@ConditionalOnExpression("${proxy.config.enable-refresh-api:false}")
@RestController
public class ConfigController {
  
  @Autowired
  private ApplicationEventPublisher publisher;
  
  @Autowired
  private ConfigFileHelper configFileHelper;
  
  @RequestMapping(value = "/api/config/refresh", method = RequestMethod.POST)
  public ResponseEntity<String> refresh() throws NoSuchAlgorithmException {
    String hash = configFileHelper.getConfigHash();
    publisher.publishEvent(new ConfigUpdateEvent(this));
    return new ResponseEntity<>(hash, HttpStatus.OK);
  }
}
