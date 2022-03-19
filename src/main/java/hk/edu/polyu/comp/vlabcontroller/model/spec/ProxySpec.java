package hk.edu.polyu.comp.vlabcontroller.model.spec;

import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProxySpec {
    private String id;
    private String displayName;
    private String description;
    private String logoURL;
    @Singular private List<String> tags = new ArrayList<>();
    @Singular private List<String> accessGroups = new ArrayList<>();
    @Singular private List<ContainerSpec> containerSpecs = new ArrayList<>();
    @Singular private List<RuntimeSettingSpec> runtimeSettingSpecs = new ArrayList<>();
    @Singular private Map<String, String> labels = new HashMap<>();
    @Setter(AccessLevel.PACKAGE) @Singular private Map<String, Object> settings = new HashMap<>();
    @Builder.Default private ProxySpecKubernetes kubernetes = new ProxySpecKubernetes();
    private boolean isSecure;
    private String defaultTutorialLink;
    private EvaluatorSpec evaluator;

    public void setContainerSpecs(List<ContainerSpec> containerSpecs) {
        this.containerSpecs = containerSpecs;
        var entryPoints = containerSpecs.stream().filter(x -> x.getEntryPoints() != null).flatMap(x -> x.getEntryPoints().stream()).collect(Collectors.toList());
        settings.put("entrypoint", entryPoints);
    }

    public void populateContainerSpecPublicPathById() {
        containerSpecs.forEach(x -> x.populatePublicPathById(id));
    }

    public ProxySpecBuilder copyToBuilder(ProxySpecBuilder builder) {
        var self = this.copy();
        self.kubernetes = self.kubernetes.copy();

        return builder
            .clearContainerSpecs()
            .clearRuntimeSettingSpecs()
            .id(id)
            .displayName(displayName)
            .description(description)
            .logoURL(logoURL)
            .accessGroups(accessGroups)
            .containerSpecs(
                self.containerSpecs.stream()
                    .peek(x -> x.populatePublicPathById(builder.id))
                    .collect(Collectors.toList())
            )
//            .runtimeSettingSpecs(self.runtimeSettingSpecs)
            .labels(labels)
            .settings(settings)
            .kubernetes(self.kubernetes)
            .defaultTutorialLink(defaultTutorialLink)
            .tags(tags)
        ;
    }

    public ProxySpecBuilder copyBuilder() {
        return this.toBuilder()
            .clearContainerSpecs()
            .clearRuntimeSettingSpecs()
            .containerSpecs(
                containerSpecs.stream()
                    .map(ContainerSpec::copy)
                    .peek(x -> x.populatePublicPathById(id))
                    .collect(Collectors.toList())
            )
//            .runtimeSettingSpecs(
//                runtimeSettingSpecs.stream()
//                    .map(RuntimeSettingSpec::copy)
//                    .collect(Collectors.toList())
//            )
            .kubernetes(kubernetes.copy());
    }

    public ProxySpec copy() {
        return copyBuilder().build();
    }
}
