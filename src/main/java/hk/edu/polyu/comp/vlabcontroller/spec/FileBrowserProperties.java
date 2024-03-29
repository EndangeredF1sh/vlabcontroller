package hk.edu.polyu.comp.vlabcontroller.spec;

import hk.edu.polyu.comp.vlabcontroller.model.spec.ProxySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.filebrowser")
public class FileBrowserProperties extends ProxySpec {

}