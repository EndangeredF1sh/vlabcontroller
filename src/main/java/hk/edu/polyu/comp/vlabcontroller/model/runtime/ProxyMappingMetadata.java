package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ToString
public class ProxyMappingMetadata {
    @Getter private URI defaultTarget;
    @Getter @Setter private List<PortMappingMetadata> portMappingMetadataList = new ArrayList<>();

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
        return portMappingMetadataList.stream().anyMatch(p -> p.getPortMapping().startsWith(prefix));
    }
}
