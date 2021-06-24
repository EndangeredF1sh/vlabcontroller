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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RefreshScope
@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "proxy.filebrowser")
public class FileBrowserProperties {
    private String containerImage;
    private String[] containerCmd;
    private Map<String,String> containerEnv;
    private String containerNetwork;
    private String[] containerVolumes;
    private String containerMemoryLimit;
    private String containerCpuLimit;
    private int port;

    private String kubernetesPodPatches;
    private List<String> kubernetesAdditionalManifests = new ArrayList<>();
    private Map<String,String> labels;

    public String getContainerImage() {
        return containerImage;
    }

    public void setContainerImage(String containerImage) {
        this.containerImage = containerImage;
    }

    public String[] getContainerCmd() {
        return containerCmd;
    }

    public void setContainerCmd(String[] containerCmd) {
        this.containerCmd = containerCmd;
    }

    public Map<String, String> getContainerEnv() {
        return containerEnv;
    }

    public void setContainerEnv(Map<String, String> containerEnv) {
        this.containerEnv = containerEnv;
    }

    public String getContainerNetwork() {
        return containerNetwork;
    }

    public void setContainerNetwork(String containerNetwork) {
        this.containerNetwork = containerNetwork;
    }

    public String[] getContainerVolumes() {
        return containerVolumes;
    }

    public void setContainerVolumes(String[] containerVolumes) {
        this.containerVolumes = containerVolumes;
    }

    public String getContainerMemoryLimit() {
        return containerMemoryLimit;
    }

    public void setContainerMemoryLimit(String containerMemoryLimit) {
        this.containerMemoryLimit = containerMemoryLimit;
    }

    public String getContainerCpuLimit() {
        return containerCpuLimit;
    }

    public void setContainerCpuLimit(String containerCpuLimit) {
        this.containerCpuLimit = containerCpuLimit;
    }

    public String getKubernetesPodPatches() {
        return kubernetesPodPatches;
    }

    public void setKubernetesPodPatches(String kubernetesPodPatches) {
        this.kubernetesPodPatches = kubernetesPodPatches;
    }

    public List<String> getKubernetesAdditionalManifests() {
        return kubernetesAdditionalManifests;
    }

    public void setKubernetesAdditionalManifests(List<String> kubernetesAdditionalManifests) {
        this.kubernetesAdditionalManifests = kubernetesAdditionalManifests;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
