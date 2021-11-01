/**
 * ShinyProxy
 * <p>
 * Copyright (C) 2016-2021 Open Analytics
 * <p>
 * ===========================================================================
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 * <p>
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
package eu.openanalytics.shinyproxy;

import eu.openanalytics.containerproxy.model.spec.ContainerSpec;
import eu.openanalytics.containerproxy.model.spec.ProxySpec;
import eu.openanalytics.containerproxy.spec.IProxySpecProvider;
import eu.openanalytics.containerproxy.util.SessionHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This component converts proxy specs from the 'ShinyProxy notation' into the 'ContainerProxy' notation.
 * ShinyProxy notation is slightly more compact, and omits several things that Shiny apps do not need,
 * such as definition of multiple containers.
 *
 * Also, if no port is specified, a port mapping is automatically created for Shiny port 3838.
 */
@Component
@Primary
@ConfigurationProperties(prefix = "shinyproxy")
public class ShinyProxySpecProvider implements IProxySpecProvider {
  
  private static Environment environment;
  @Getter @Setter private List<ProxySpec> specs = new ArrayList<>();
  
  private static String getPublicPath(String appName) {
    String contextPath = SessionHelper.getContextPath(environment, true);
    return contextPath + "app_direct/" + appName + "/";
  }
  
  public static ProxySpec getProxySpec(ProxySpec to, ContainerSpec cSpec, Map<String, String> labels, int port) {
    cSpec.setLabels(labels);
    
    Map<String, Integer> portMapping = new HashMap<>();
    if (port > 0) {
      portMapping.put("default", port);
    } else {
      portMapping.put("default", 3838);
    }
    cSpec.setPortMapping(portMapping);
    
    to.setContainerSpecs(Collections.singletonList(cSpec));
    
    return to;
  }
  
  @Autowired
  public void setEnvironment(Environment env) {
    ShinyProxySpecProvider.environment = env;
  }
  
  @PostConstruct
  public void afterPropertiesSet() {
    this.specs.stream().collect(Collectors.groupingBy(ProxySpec::getId)).forEach((id, duplicateSpecs) -> {
      if (duplicateSpecs.size() > 1)
        throw new IllegalArgumentException(String.format("Configuration error: spec with id '%s' is defined multiple times", id));
    });
  }
  
  public ProxySpec getSpec(String id) {
    if (id == null || id.isEmpty()) return null;
    return specs.stream().filter(s -> id.equals(s.getId())).findAny().orElse(null);
  }
}
