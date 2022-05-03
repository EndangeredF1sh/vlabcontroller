package hk.edu.polyu.comp.vlabcontroller.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "server")
@AllArgsConstructor
@NoArgsConstructor
public class ServerProperties {
    String frameOptions = "disable";
    @Value("${server.servlet.context-path:}") String servletContextPath;
    boolean secureCookies = false;
    boolean useForwardHeaders;
}
