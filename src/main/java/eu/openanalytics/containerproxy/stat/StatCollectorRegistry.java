/**
 * ContainerProxy
 *
 * Copyright (C) 2016-2021 Open Analytics
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
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
	
	@Inject
	private Environment environment;

	@Inject
	private ApplicationContext applicationContext;

	@Inject
	private StatCollectorProperties statCollectorProperties;

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
			defaultListableBeanFactory.registerBeanDefinition(klass.getName()+"Bean", beanDefinitionBuilder.getBeanDefinition());
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
