package hk.edu.polyu.comp.vlabcontroller.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Objects;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyUsageStatsProperties {
    String username = "monetdb";
    String password = "monetdb";
    @NestedConfigurationProperty ProxyUsageStatsPropertiesUrls url = new ProxyUsageStatsPropertiesUrls();
    @NestedConfigurationProperty ProxyUsageStatsHikariProperties hikari = new ProxyUsageStatsHikariProperties();

    @Data public static class ProxyUsageStatsPropertiesUrls {
        String influx = "";
        String jdbc = "";
        String micrometer = "";

        public boolean backendExists() {
            return !Stream.of(influx, jdbc, micrometer).filter(Objects::nonNull).allMatch(String::isBlank);
        }
    }
}
