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
package eu.openanalytics.containerproxy.spec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.usage-stats-url")
public class StatCollectorProperties {
    private String influxURL = "";
    private String jdbcURL = "";
    private String micrometerURL = "";

    public String getInfluxURL() {
        return influxURL;
    }

    public void setInfluxURL(String influxURL) {
        this.influxURL = influxURL;
    }

    public String getJdbcURL() {
        return jdbcURL;
    }

    public void setJdbcURL(String jdbcURL) {
        this.jdbcURL = jdbcURL;
    }

    public String getMicrometerURL() {
        return micrometerURL;
    }

    public void setMicrometerURL(String micrometerURL) {
        this.micrometerURL = micrometerURL;
    }

    public boolean backendExists(){
        return !influxURL.isEmpty() || !jdbcURL.isEmpty() || !micrometerURL.isEmpty();
    }
}
