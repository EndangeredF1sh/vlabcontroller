package hk.edu.polyu.comp.vlabcontroller.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class LabInstance {
    @Id private String id;
    @CreatedDate private DateTime startedAt;
    private DateTime completedAt;
    @Builder.Default private Set<String> progress = new HashSet<>();
}
