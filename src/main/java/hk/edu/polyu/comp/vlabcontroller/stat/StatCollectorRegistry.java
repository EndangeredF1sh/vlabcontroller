package hk.edu.polyu.comp.vlabcontroller.stat;

import hk.edu.polyu.comp.vlabcontroller.config.ProxyProperties;
import hk.edu.polyu.comp.vlabcontroller.stat.impl.InfluxDBCollector;
import hk.edu.polyu.comp.vlabcontroller.stat.impl.JDBCCollector;
import hk.edu.polyu.comp.vlabcontroller.stat.impl.Micrometer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@RefreshScope
class StatCollectorFactory {
    private final ApplicationContext applicationContext;
    private final ProxyProperties proxyProperties;

    @Bean
    public IStatCollector statsCollector() {
        // create beans manually, spring will not create beans automatically when null returned
        var url = proxyProperties.getUsageStats().getUrl();
        if (!url.backendExists()) {
            log.info("Disabled. Usage statistics will not be processed.");
            return null;
        }

        var configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        var defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getAutowireCapableBeanFactory();

        var createBean = (Consumer<Class<?>>) (Class<?> klass) -> {
            var beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(klass);
            defaultListableBeanFactory.registerBeanDefinition(klass.getName() + "Bean", beanDefinitionBuilder.getBeanDefinition());
        };

        if (url.getInflux().contains("/write?db=")) {
            createBean.accept(InfluxDBCollector.class);
            log.info("Influx DB backend enabled, sending usage statics to {}", url.getInflux());
        }
        if (url.getJdbc().contains("jdbc")) {
            createBean.accept(JDBCCollector.class);
            log.info("JDBC backend enabled, sending usage statistics to {}", url.getJdbc());

        }
        if (url.getMicrometer().contains("micrometer")) {
            createBean.accept(Micrometer.class);
            log.info("Prometheus (Micrometer) backend enabled");
        }

        return null;
    }
}
