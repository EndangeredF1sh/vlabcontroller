package eu.openanalytics.containerproxy.model.spec;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

public class ProxyAccessControl {
  @Getter @Setter private String[] groups;
  
  public void copy(ProxyAccessControl target) {
    if (groups != null) {
      target.setGroups(Arrays.copyOf(groups, groups.length));
    }
  }
}
