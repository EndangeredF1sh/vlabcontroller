package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ToString
@Data @Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProxyMappingMetadata {
    @Setter(AccessLevel.NONE) private URI defaultTarget;
    @Singular("portMappingMetadata") private List<PortMappingMetadata> portMappingMetadataList = new ArrayList<>();

    public void setDefaultTarget(URI defaultTarget) {
        // Can't be updated if set
        if (this.defaultTarget == null) {
            this.defaultTarget = defaultTarget;
        }
    }

    public boolean containsExactMappingPath(String path) {
        return portMappingMetadataList.stream().anyMatch(p -> p.getPortMapping().equals(path));
    }

    public boolean containsMappingPathPrefix(String prefix) {
        var path = StringUtils.removeEnd(prefix, "/");
        return portMappingMetadataList.stream().anyMatch(p -> p.getPortMapping().startsWith(path));
    }
}
