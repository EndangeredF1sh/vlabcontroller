package hk.edu.polyu.comp.vlabcontroller.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.IDToken;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyKeycloakProperties {
    String realm;
    String claim;
    String authServerUrl;
    String resource;
    String sslRequired = "external";
    boolean useResourceRoleMappings = false;
    String credentialsSecret;
    String nameAttribute = IDToken.NAME;
}
