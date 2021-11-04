package eu.openanalytics.containerproxy.stat;

import eu.openanalytics.containerproxy.spec.StatCollectorProperties;
import eu.openanalytics.containerproxy.stat.impl.InfluxDBCollector;
import eu.openanalytics.containerproxy.stat.impl.JDBCCollector;
import eu.openanalytics.containerproxy.stat.impl.Micrometer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.util.function.Consumer;

@Configuration
class StatCollectorFactory {
  
  private final Logger log = LogManager.getLogger(StatCollectorFactory.class);
  
  private final Environment environment;
  private final ApplicationContext applicationContext;
  private final StatCollectorProperties statCollectorProperties;
  
  public StatCollectorFactory(Environment environment, ApplicationContext applicationContext, StatCollectorProperties statCollectorProperties) {
    this.environment = environment;
    this.applicationContext = applicationContext;
    this.statCollectorProperties = statCollectorProperties;
  }
  
  @Bean
  public IStatCollector statsCollector() {
    // create beans manually, spring will not create beans automatically when null returned
    if (!statCollectorProperties.backendExists()) {
      log.info("Disabled. Usage statistics will not be processed.");
      return null;
    }
    
    ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
    DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getAutowireCapableBeanFactory();
    
    Consumer<Class<?>> createBean = (Class<?> klass) -> {
      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(klass);
      defaultListableBeanFactory.registerBeanDefinition(klass.getName() + "Bean", beanDefinitionBuilder.getBeanDefinition());
    };
    
    if (statCollectorProperties.getInfluxURL().contains("/write?db=")) {
      createBean.accept(InfluxDBCollector.class);
      log.info("Influx DB backend enabled, sending usage statics to {}", statCollectorProperties.getInfluxURL());
    }
    if (statCollectorProperties.getJdbcURL().contains("jdbc")) {
      createBean.accept(JDBCCollector.class);
      log.info("JDBC backend enabled, sending usage statistics to {}", statCollectorProperties.getJdbcURL());
      
    }
    if (statCollectorProperties.getMicrometerURL().contains("micrometer")) {
      createBean.accept(Micrometer.class);
      log.info("Prometheus (Micrometer) backend enabled");
    }
    
    return null;
  }
}
