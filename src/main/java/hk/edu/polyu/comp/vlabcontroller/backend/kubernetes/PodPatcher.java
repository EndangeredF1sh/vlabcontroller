package hk.edu.polyu.comp.vlabcontroller.backend.kubernetes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;
import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.json.JsonPatch;
import javax.json.JsonStructure;

@Slf4j
@Component
@RequiredArgsConstructor
@RefreshScope
public class PodPatcher {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProxyProperties proxyProperties;

    public boolean isLoggingEnabled() {
        return proxyProperties.getKubernetes().isDebugPatches();
    }

    @PostConstruct
    public void init() {
        mapper.registerModule(new JSR353Module());
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
        var podAsJsonValue = mapper.convertValue(pod, JsonStructure.class);
        // 2. apply patch
        var patchedPodAsJsonValue = patch.apply(podAsJsonValue);
        // 3. convert back to a pod
        return mapper.convertValue(patchedPodAsJsonValue, Pod.class);
    }

    /**
     * Applies a JsonPatch to the given Pod. When proxy.kubernetes.debug-patches is
     * enabled the original and patched specification will be logged as YAML.
     */
    public Pod patchWithDebug(Pod pod, JsonPatch patch) throws JsonProcessingException {
        if (isLoggingEnabled()) {
            log.info("Original Pod: " + SerializationUtils.dumpAsYaml(pod));
        }
        var patchedPod = patch(pod, patch);
        if (isLoggingEnabled()) {
            log.info("Patched Pod: " + SerializationUtils.dumpAsYaml(patchedPod));
        }
        return patchedPod;
    }

}
