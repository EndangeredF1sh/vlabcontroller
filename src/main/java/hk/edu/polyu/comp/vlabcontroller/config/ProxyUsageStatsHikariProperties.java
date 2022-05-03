package hk.edu.polyu.comp.vlabcontroller.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyUsageStatsHikariProperties {
    Duration connectionTimeout;
    Duration idleTimeout;
    Duration maxLifetime;
    int minimumIdle;
    int maximumPoolSize = 1;
}
