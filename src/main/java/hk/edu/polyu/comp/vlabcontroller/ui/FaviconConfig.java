package hk.edu.polyu.comp.vlabcontroller.ui;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
@RefreshScope
public class FaviconConfig {
    private static final String CONTENT_TYPE_ICO = "image/x-icon";
    private final ProxyProperties proxyProperties;

    @Bean
    @ConditionalOnProperty(name = "proxy.favicon-path")
    public SimpleUrlHandlerMapping customFaviconHandlerMapping() {
        byte[] cachedIcon = null;

        var iconPath = Paths.get(proxyProperties.getFaviconPath());
        if (Files.isRegularFile(iconPath)) {
            try (var input = Files.newInputStream(iconPath)) {
                cachedIcon = FileCopyUtils.copyToByteArray(input);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read favicon: " + iconPath, e);
            }
        } else {
            log.error("Invalid favicon path: " + iconPath);
        }

        var mapping = new SimpleUrlHandlerMapping();
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
            var fileName = iconPath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".ico")) return CONTENT_TYPE_ICO;

            var mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
            return mediaType.toString();
        }
    }

}
