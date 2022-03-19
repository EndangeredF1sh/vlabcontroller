package hk.edu.polyu.comp.vlabcontroller.model.spec;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Volume;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data @Builder(toBuilder = true)
@AllArgsConstructor @NoArgsConstructor
public class ProxySpecKubernetes {
    @Singular private List<Volume> volumes = new ArrayList<>();
    private String podPatches;
    @Singular private List<String> additionalManifests = new ArrayList<>();
    @Singular private List<PersistentVolumeClaim> persistentVolumeClaims = new ArrayList<>();

    public ProxySpecKubernetesBuilder copyToBuilder(ProxySpecKubernetesBuilder builder) {
        return builder
            .volumes(volumes)
            .podPatches(podPatches)
            .additionalManifests(additionalManifests)
            .persistentVolumeClaims(persistentVolumeClaims);
    }

    public ProxySpecKubernetesBuilder copyBuilder() {
        return this.toBuilder();
    }

    public ProxySpecKubernetes copy() {
        return copyBuilder().build();
    }
}
