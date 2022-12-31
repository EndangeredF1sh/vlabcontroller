package hk.edu.polyu.comp.vlabcontroller.model.spec;

import hk.edu.polyu.comp.vlabcontroller.spec.impl.DefaultSpecProvider;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProxySpec {
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String displayName;
    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
    private String logoURL;
    @Getter
    @Setter
    private List<String> accessGroups = new ArrayList<>();
    @Getter
    private List<ContainerSpec> containerSpecs = new ArrayList<>();
    @Getter
    @Setter
    private List<RuntimeSettingSpec> runtimeSettingSpecs = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, String> labels = new HashMap<>();
    @Getter
    private Map<String, Object> settings = new HashMap<>();
    @Getter
    @Setter
    private ProxySpecKubernetes kubernetes = new ProxySpecKubernetes();
    @Getter
    @Setter
    private String defaultTutorialLink;
    @Getter
    @Setter
    private String templateName = "";

    public void setContainerSpecs(List<ContainerSpec> containerSpecs) {
        this.containerSpecs = containerSpecs;
        var entryPoints = containerSpecs.stream().flatMap(x -> x.getEntryPoints().stream()).collect(Collectors.toList());
        settings.put("entrypoint", entryPoints);
    }

    public void copy(ProxySpec target) {
        target.setId(id);
        target.setDisplayName(displayName);
        target.setDescription(description);
        target.setLogoURL(logoURL);
        target.setDefaultTutorialLink(defaultTutorialLink);
        target.setTemplateName(templateName);

        target.getAccessGroups().addAll(accessGroups);

        for (ContainerSpec spec : containerSpecs) {
            ContainerSpec copy = new ContainerSpec();
            spec.copy(copy);
            copy.getEnv().put("PUBLIC_PATH", DefaultSpecProvider.getPublicPath(id));
            target.getContainerSpecs().add(copy);
        }

        for (RuntimeSettingSpec spec : runtimeSettingSpecs) {
            RuntimeSettingSpec copy = new RuntimeSettingSpec();
            spec.copy(copy);
            target.getRuntimeSettingSpecs().add(copy);
        }

        target.getLabels().putAll(labels);
        target.getSettings().putAll(settings);
        ProxySpecKubernetes proxySpecKubernetesCopy = new ProxySpecKubernetes();
        kubernetes.copy(proxySpecKubernetesCopy);
        target.setKubernetes(proxySpecKubernetesCopy);
    }

}
