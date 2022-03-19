package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ProxyMappingMetadataTest {

    @Test
    public void testContainsExactTargetPath() throws URISyntaxException {
        var metadata = ProxyMappingMetadata.builder()
                .portMappingMetadataList(List.of(
                        PortMappingMetadata.builder()
                                .portMapping("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8000")
                                .target(new URI("http://10.42.61.11:8000"))
                                .build(),
                        PortMappingMetadata.builder()
                                .portMapping("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080")
                                .target(new URI("http://10.42.61.11:8080"))
                                .build()
                ))
                .build();
        assertFalse(metadata.containsExactMappingPath("test"));
        assertTrue(metadata.containsExactMappingPath("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8000"));
        assertTrue(metadata.containsExactMappingPath("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080"));
    }

    @Test
    public void testContainsMappingPathPrefix() throws URISyntaxException {
        var metadata = ProxyMappingMetadata.builder()
                .portMappingMetadataList(List.of(
                        PortMappingMetadata.builder()
                                .portMapping("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8000")
                                .target(new URI("http://10.42.61.11:8000"))
                                .build(),
                        PortMappingMetadata.builder()
                                .portMapping("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080")
                                .target(new URI("http://10.42.61.11:8080"))
                                .build()
                ))
                .build();
        assertFalse(metadata.containsMappingPathPrefix("test"));
        assertFalse(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8087"));
        assertFalse(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port8087"));
        assertTrue(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd"));
        assertTrue(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings"));
        assertTrue(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080"));
        assertTrue(metadata.containsMappingPathPrefix("1ca3dde2-8fdf-4fe4-8327-6849e4d77fcd/port_mappings/8080/"));
    }
}