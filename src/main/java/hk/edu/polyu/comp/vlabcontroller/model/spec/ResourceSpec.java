package hk.edu.polyu.comp.vlabcontroller.model.spec;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ResourceSpec {
    @Singular Map<String, Quantity> limits = new HashMap<>();
    @Singular Map<String, Quantity> requests = new HashMap<>();

    public ResourceRequirements asResourceRequirements() {
        return new ResourceRequirementsBuilder()
            .addToRequests(getRequests())
            .addToLimits(getLimits())
        .build();
    }
}
