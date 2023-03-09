package hk.edu.polyu.comp.vlabcontroller.api;

import hk.edu.polyu.comp.vlabcontroller.event.ConfigUpdateEvent;
import hk.edu.polyu.comp.vlabcontroller.model.runtime.API.APIResponseBody;
import hk.edu.polyu.comp.vlabcontroller.util.ConfigFileHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@ConditionalOnExpression("${proxy.config.enable-refresh-api:false}")
@RestController
public class ConfigController extends BaseController {
    private final ApplicationEventPublisher publisher;
    private final ConfigFileHelper configFileHelper;

    public ConfigController(ApplicationEventPublisher publisher, ConfigFileHelper configFileHelper) {
        this.publisher = publisher;
        this.configFileHelper = configFileHelper;
    }

    @PostMapping(value = "/api/config/refresh")
    public ResponseEntity<APIResponseBody<Map<String, String>>> refresh() throws NoSuchAlgorithmException {
        String hash = configFileHelper.getConfigHash();
        publisher.publishEvent(new ConfigUpdateEvent(this));
        Map<String, String> map = new HashMap<>();
        map.put("configHash", hash);
        return APIResponseBody.success(map);
    }
}
