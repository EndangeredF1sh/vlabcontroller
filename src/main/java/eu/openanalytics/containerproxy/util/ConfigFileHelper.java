package eu.openanalytics.containerproxy.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import eu.openanalytics.containerproxy.ContainerProxyApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class ConfigFileHelper {
  private final Environment environment;
  
  public ConfigFileHelper(Environment environment) {
    this.environment = environment;
  }
  
  private File getConfigFile() {
    String path = environment.getProperty("spring.config.location");
    path = path == null ? ContainerProxyApplication.CONFIG_FILENAME : path;
    File file = Paths.get(path).toFile();
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
    try {
      Object parsedConfig = objectMapper.readValue(file, Object.class);
      String canonicalConfigFile = objectMapper.writeValueAsString(parsedConfig);
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.reset();
      digest.update(canonicalConfigFile.getBytes(Charsets.UTF_8));
      configHash = String.format("%040x", new BigInteger(1, digest.digest()));
      return configHash;
    } catch (IOException e) {
      return "illegal";
    }
  }
}
