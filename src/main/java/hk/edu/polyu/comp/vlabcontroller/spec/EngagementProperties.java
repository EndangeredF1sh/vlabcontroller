package hk.edu.polyu.comp.vlabcontroller.spec;


import lombok.Getter;
import lombok.Setter;
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
    @Getter
    @Setter
    private boolean enabled = true;
    @Getter
    @Setter
    private List<String> filterPath = new ArrayList<>();
    @Getter
    @Setter
    private int idleRetry = 3;
    @Getter
    @Setter
    private int threshold = 230;
    @Getter
    private Duration maxAge = Duration.ofHours(4);

    public void setMaxAge(String duration) {
        this.maxAge = Duration.parse(duration);
    }
}
