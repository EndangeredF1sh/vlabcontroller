package eu.openanalytics.containerproxy.model.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.openanalytics.containerproxy.model.spec.ContainerSpec;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class Container {
  
  @Getter @Setter private String id;
  @Getter @Setter private ContainerSpec spec;
  @Setter private Map<String, Object> parameters = new HashMap<>();
  
  @JsonIgnore
  public Map<String, Object> getParameters() {
    return parameters;
  }
}
