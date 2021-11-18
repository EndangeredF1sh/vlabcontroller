package eu.openanalytics.containerproxy.model.spec;

import io.fabric8.kubernetes.api.model.Volume;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class ProxySpecKubernetes {
    @Getter
    @Setter
    private List<Volume> volumes = new ArrayList<>();
    @Getter
    @Setter
    private String podPatches;
    @Getter
    @Setter
    private List<String> additionalManifests = new ArrayList<>();

    public void copy(ProxySpecKubernetes target){
        target.getVolumes().addAll(volumes);
        target.setPodPatches(podPatches);
        target.getAdditionalManifests().addAll(additionalManifests);
    }
}
