package hk.edu.polyu.comp.vlabcontroller.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyOpenIDProperties {
    String logoutUrl;
    String usernameAttribute = "email";
    String authUrl;
    String tokenUrl;
    String jwksUrl;
    String clientId;
    String clientSecret;
    String rolesClaim;
    List<String> scopes = new ArrayList<>();
}
