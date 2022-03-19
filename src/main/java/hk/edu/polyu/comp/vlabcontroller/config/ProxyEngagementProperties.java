package hk.edu.polyu.comp.vlabcontroller.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyEngagementProperties {
    boolean enabled = true;
    List<String> filterPath = new ArrayList<>();
    int idleRetry = 3;
    int threshold = 230;
    Duration maxAge = Duration.ofHours(4);
}
