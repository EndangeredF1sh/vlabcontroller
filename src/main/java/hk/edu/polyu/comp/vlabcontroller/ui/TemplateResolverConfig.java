package hk.edu.polyu.comp.vlabcontroller.ui;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.templateresolver.FileTemplateResolver;

@Configuration
@RequiredArgsConstructor
@RefreshScope
public class TemplateResolverConfig implements WebMvcConfigurer {
    private final ProxyProperties proxyProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("file:" + proxyProperties.getTemplatePath() + "/assets/");
    }

    @Bean
    public FileTemplateResolver templateResolver() {
        return new FileTemplateResolver() {{
            setPrefix(proxyProperties.getTemplatePath() + "/");
            setSuffix(".html");
            setTemplateMode("HTML5");
            setCacheable(false);
            setCheckExistence(true);
            setOrder(1);
        }};
    }
}
