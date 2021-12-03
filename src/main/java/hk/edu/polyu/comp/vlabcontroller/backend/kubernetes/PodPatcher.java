package hk.edu.polyu.comp.vlabcontroller.backend.kubernetes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.JsonPatch;
import javax.json.JsonStructure;

@Component
public class PodPatcher {

    private static final String DEBUG_PROPERTY = "proxy.kubernetes.debug-patches";
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LogManager.getLogger(getClass());
    private final Environment environment;
    private boolean loggingEnabled = false;

    public PodPatcher(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        mapper.registerModule(new JSR353Module());
        loggingEnabled = Boolean.valueOf(environment.getProperty(DEBUG_PROPERTY, "false"));
    }

    /**
     * Applies a JsonPatch to the given Pod.
     */
    public Pod patch(Pod pod, JsonPatch patch) {
        if (patch == null) {
            return pod;
        }
        // 1. convert Pod to javax.json.JsonValue object.
        // This conversion does not actually convert to a string, but some internal
        // representation of Jackson.
        JsonStructure podAsJsonValue = mapper.convertValue(pod, JsonStructure.class);
        // 2. apply patch
        JsonStructure patchedPodAsJsonValue = patch.apply(podAsJsonValue);
        // 3. convert back to a pod
        return mapper.convertValue(patchedPodAsJsonValue, Pod.class);
    }

    /**
     * Applies a JsonPatch to the given Pod. When proxy.kubernetes.debug-patches is
     * enabled the original and patched specification will be logged as YAML.
     */
    public Pod patchWithDebug(Pod pod, JsonPatch patch) throws JsonProcessingException {
        if (loggingEnabled) {
            log.info("Original Pod: " + SerializationUtils.dumpAsYaml(pod));
        }
        Pod patchedPod = patch(pod, patch);
        if (loggingEnabled) {
            log.info("Patched Pod: " + SerializationUtils.dumpAsYaml(patchedPod));
        }
        return patchedPod;
    }

}
