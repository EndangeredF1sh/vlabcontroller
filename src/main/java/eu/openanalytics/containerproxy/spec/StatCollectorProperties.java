package eu.openanalytics.containerproxy.spec;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.usage-stats-url")
public class StatCollectorProperties {
    @Getter
    @Setter
    private String influxURL = "";
    @Getter
    @Setter
    private String jdbcURL = "";
    @Getter
    @Setter
    private String micrometerURL = "";

    public boolean backendExists() {
        return !influxURL.isEmpty() || !jdbcURL.isEmpty() || !micrometerURL.isEmpty();
    }
}
