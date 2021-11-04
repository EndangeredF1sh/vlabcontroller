package eu.openanalytics.containerproxy.ui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import javax.inject.Inject;

@Configuration
public class TemplateResolverConfig implements WebMvcConfigurer {
  private final Environment environment;
  
  public TemplateResolverConfig(Environment environment) {
    this.environment = environment;
  }
  
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/assets/**")
      .addResourceLocations("file:" + environment.getProperty("proxy.template-path") + "/assets/");
  }
  
  @Bean
  public FileTemplateResolver templateResolver() {
    FileTemplateResolver resolver = new FileTemplateResolver();
    resolver.setPrefix(environment.getProperty("proxy.template-path") + "/");
    
    resolver.setSuffix(".html");
    resolver.setTemplateMode("HTML5");
    resolver.setCacheable(false);
    resolver.setCheckExistence(true);
    resolver.setOrder(1);
    return resolver;
  }
}
