package hk.edu.polyu.comp.vlabcontroller.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import hk.edu.polyu.comp.vlabcontroller.VLabControllerApplication;
import io.vavr.CheckedFunction1;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConfigFileHelper {
    private final Environment environment;

    private File getConfigFile() {
        return Optional.ofNullable(environment.getProperty("spring.config.location"))
            .or(() -> Optional.of(VLabControllerApplication.CONFIG_FILENAME))
            .map(path -> Paths.get(path).toFile())
            .filter(File::exists)
            .orElse(null);
    }

    public String getConfigHash() {
        var objectMapper = new ObjectMapper(new YAMLFactory()) {{
            configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        }};
        return Optional.ofNullable(getConfigFile())
            .map(CheckedFunction1.lift(file -> {
                var parsedConfig = objectMapper.readValue(file, Object.class);
                var canonicalConfigFile = objectMapper.writeValueAsString(parsedConfig);
                var digest = MessageDigest.getInstance("SHA-1");
                digest.reset();
                digest.update(canonicalConfigFile.getBytes(StandardCharsets.UTF_8));
                return String.format("%040x", new BigInteger(1, digest.digest()));
            }))
            .map(x -> x.getOrElse("illegal"))
            .orElse("unknown");
    }
}
