package eu.openanalytics.containerproxy.spec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.usage-stats-url")
public class StatCollectorProperties {
  private String influxURL = "";
  private String jdbcURL = "";
  private String micrometerURL = "";
  
  public String getInfluxURL() {
    return influxURL;
  }
  
  public void setInfluxURL(String influxURL) {
    this.influxURL = influxURL;
  }
  
  public String getJdbcURL() {
    return jdbcURL;
  }
  
  public void setJdbcURL(String jdbcURL) {
    this.jdbcURL = jdbcURL;
  }
  
  public String getMicrometerURL() {
    return micrometerURL;
  }
  
  public void setMicrometerURL(String micrometerURL) {
    this.micrometerURL = micrometerURL;
  }
  
  public boolean backendExists() {
    return !influxURL.isEmpty() || !jdbcURL.isEmpty() || !micrometerURL.isEmpty();
  }
}
