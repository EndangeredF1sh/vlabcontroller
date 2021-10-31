package eu.openanalytics.containerproxy.spec;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.engagement")
public class EngagementProperties {
  private boolean enabled = true;
  private List<String> filterPath = new ArrayList<>();
  private int idleRetry = 3;
  private int threshold = 230;
  private Duration maxAge = Duration.ofHours(4);
  
  public boolean isEnabled() {
    return enabled;
  }
  
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  public Duration getMaxAge() {
    return maxAge;
  }
  
  public void setMaxAge(String duration) {
    this.maxAge = Duration.parse(duration);
  }
  
  public List<String> getFilterPath() {
    return filterPath;
  }
  
  public void setFilterPath(List<String> filterPath) {
    this.filterPath = filterPath;
  }
  
  public int getIdleRetry() {
    return idleRetry;
  }
  
  public void setIdleRetry(int idleRetry) {
    this.idleRetry = idleRetry;
  }
  
  public int getThreshold() {
    return threshold;
  }
  
  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }
}
