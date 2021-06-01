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

package eu.openanalytics.containerproxy.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException;
import com.google.common.base.Charsets;
import eu.openanalytics.containerproxy.ContainerProxyApplication;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class ConfigFileHelper {
    private File getConfigFile(){
        File file = Paths.get(ContainerProxyApplication.CONFIG_FILENAME).toFile();
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public String getConfigHash() throws NoSuchAlgorithmException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        File file = getConfigFile();
        String configHash;
        if (file == null) {
            configHash = "unknown";
            return configHash;
        }
        try{
            Object parsedConfig = objectMapper.readValue(file, Object.class);
            String canonicalConfigFile =  objectMapper.writeValueAsString(parsedConfig);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(canonicalConfigFile.getBytes(Charsets.UTF_8));
            configHash = String.format("%040x", new BigInteger(1, digest.digest()));
            return configHash;
        }catch (IOException e){
            return "illegal";
        }
    }
}
