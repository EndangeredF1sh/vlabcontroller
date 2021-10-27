package eu.openanalytics.containerproxy.ui;

import org.apache.logging.log4j.LogManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@Configuration
public class FaviconConfig {
  
  private static final String CONTENT_TYPE_ICO = "image/x-icon";
  
  @Inject
  private Environment environment;
  
  @Bean
  @ConditionalOnProperty(name = "proxy.favicon-path")
  public SimpleUrlHandlerMapping customFaviconHandlerMapping() {
    byte[] cachedIcon = null;
    
    Path iconPath = Paths.get(environment.getProperty("proxy.favicon-path"));
    if (Files.isRegularFile(iconPath)) {
      try (InputStream input = Files.newInputStream(iconPath)) {
        cachedIcon = FileCopyUtils.copyToByteArray(input);
      } catch (IOException e) {
        throw new IllegalArgumentException("Cannot read favicon: " + iconPath, e);
      }
    } else {
      LogManager.getLogger(FaviconConfig.class).error("Invalid favicon path: " + iconPath);
    }
    
    SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
    mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
    mapping.setUrlMap(Collections.singletonMap("**/favicon.???", new CachedFaviconHttpRequestHandler(cachedIcon, iconPath)));
    return mapping;
  }
  
  private static class CachedFaviconHttpRequestHandler implements HttpRequestHandler {
    
    private final byte[] cachedIcon;
    private final Path iconPath;
    
    public CachedFaviconHttpRequestHandler(byte[] cachedIcon, Path iconPath) {
      this.cachedIcon = cachedIcon;
      this.iconPath = iconPath;
    }
    
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      response.setHeader("Content-Type", getContentType());
      response.setHeader("Content-Length", String.valueOf(cachedIcon.length));
      response.getOutputStream().write(cachedIcon);
      response.getOutputStream().flush();
      response.setStatus(200);
    }
    
    private String getContentType() {
      String fileName = iconPath.getFileName().toString().toLowerCase();
      if (fileName.endsWith(".ico")) return CONTENT_TYPE_ICO;
      
      MediaType mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
      return mediaType.toString();
    }
  }
  
}
