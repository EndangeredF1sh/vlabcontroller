package eu.openanalytics.containerproxy.model.spec;

import eu.openanalytics.containerproxy.spec.impl.DefaultSpecProvider;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Setter
    private List<ContainerSpec> containerSpecs = new ArrayList<>();
    @Getter
    @Setter
    private List<RuntimeSettingSpec> runtimeSettingSpecs = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, String> labels = new HashMap<>();
    @Getter
    @Setter
    private Map<String, String> settings = new HashMap<>();
    @Getter
    @Setter
    private ProxySpecKubernetes kubernetes = new ProxySpecKubernetes();
    @Getter
    @Setter
    private List<SubApplicationSpec> subApps = new ArrayList<>();
    @Getter
    @Setter
    private String defaultTutorialLink;

    public void copy(ProxySpec target) {
        target.setId(id);
        target.setDisplayName(displayName);
        target.setDescription(description);
        target.setLogoURL(logoURL);
        target.setDefaultTutorialLink(defaultTutorialLink);

        target.getAccessGroups().addAll(accessGroups);

        for (ContainerSpec spec : containerSpecs) {
            ContainerSpec copy = new ContainerSpec();
            spec.copy(copy);
            copy.getEnv().put("SHINYPROXY_PUBLIC_PATH", DefaultSpecProvider.getPublicPath(id));
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

        target.getSubApps().addAll(subApps);
    }

}
