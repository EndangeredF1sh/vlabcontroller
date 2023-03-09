package hk.edu.polyu.comp.vlabcontroller.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CrossOriginSecurityConfig implements ICustomSecurityConfig {
    private final Logger log = LogManager.getLogger(CrossOriginSecurityConfig.class);

    @Autowired
    private Environment environment;

    @Override
    public void apply(HttpSecurity http) throws Exception {
        http.cors();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String originPattern = environment.getProperty("proxy.cors.origin-pattern", "*");
        configuration.addAllowedOriginPattern(originPattern);
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        String pathPattern = environment.getProperty("proxy.cors.path-pattern", "/api/**");
        source.registerCorsConfiguration(pathPattern, configuration);
        log.info("Enable CORS Configurations for '{}', originPattern='{}'", pathPattern, originPattern);
        return source;
    }
}
