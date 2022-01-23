package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

class ProxyMappingMetadataTest {

    @Test
    void containsExactTargetPath() throws URISyntaxException {
        var metadata = new ProxyMappingMetadata();
        Assertions.assertFalse(metadata.containsExactMappingPath("test"));
        metadata.getPortMappingMetadataList().add(
                new PortMappingMetadata(
                        "1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8000",
                        new URI("http://10.42.61.11:8000"),
                        null
                ));
        metadata.getPortMappingMetadataList().add(
                new PortMappingMetadata(
                        "1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080",
                        new URI("http://10.42.61.11:8080"),
                        null
                ));
        Assertions.assertFalse(metadata.containsExactMappingPath("test"));
        Assertions.assertTrue(metadata.containsExactMappingPath("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8000"));
        Assertions.assertTrue(metadata.containsExactMappingPath("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080"));
    }

    @Test
    void containsMappingPathPrefix() throws URISyntaxException {
        var metadata = new ProxyMappingMetadata();
        Assertions.assertFalse(metadata.containsMappingPathPrefix("test"));
        metadata.getPortMappingMetadataList().add(
                new PortMappingMetadata(
                        "1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8000",
                        new URI("http://10.42.61.11:8000"),
                        null
                ));
        metadata.getPortMappingMetadataList().add(
                new PortMappingMetadata(
                        "1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080",
                        new URI("http://10.42.61.11:8080"),
                        null
                ));
        Assertions.assertFalse(metadata.containsMappingPathPrefix("test"));
        Assertions.assertFalse(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8087"));
        Assertions.assertFalse(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port8087"));
        Assertions.assertTrue(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd"));
        Assertions.assertTrue(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings"));
        Assertions.assertTrue(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080"));
    }
}