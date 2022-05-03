package hk.edu.polyu.comp.vlabcontroller.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyUserProperties {
    String name;
    String password;
    List<String> roles = new ArrayList<>();
    List<String> groups = new ArrayList<>();
}
