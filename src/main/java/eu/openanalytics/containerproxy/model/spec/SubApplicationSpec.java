package eu.openanalytics.containerproxy.model.spec;

import lombok.Getter;
import lombok.Setter;

public class SubApplicationSpec {
  @Getter @Setter private String path = "";
  @Getter @Setter private String subDomain = "";
  @Getter @Setter private String displayName;
  @Getter @Setter private String description;
}
