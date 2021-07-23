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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.engagement")
public class EngagementProperties {
    private boolean enabled = true;
    private Set<Integer> filterHeader;
    private List<String> filterPath = new ArrayList<>();
    private long automaticTimeout = 900000;
    private Duration maxAge = Duration.ofHours(4);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public long getAutomaticTimeout() {
        return automaticTimeout;
    }

    public void setAutomaticTimeout(long automaticTimeout) {
        this.automaticTimeout = automaticTimeout;
    }

    public void setFilterHeader(List<Integer> filterHeader){
        if (filterHeader != null){
            this.filterHeader = new HashSet<>(filterHeader);
        }else{
            this.filterHeader = new HashSet<>();
        }
    }

    public Set<Integer> getFilterHeader(){
        return this.filterHeader;
    }
    public void setMaxAge(String duration){
        this.maxAge = Duration.parse(duration);
    }
    public Duration getMaxAge(){
        return maxAge;
    }
    public List<String> getFilterPath() {
        return filterPath;
    }
    public void setFilterPath(List<String> filterPath) {
        this.filterPath = filterPath;
    }
}
