package hk.edu.polyu.comp.vlabcontroller.model.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EvaluatorSpec extends ContainerSpec {
    @Singular
    private List<String> goals = new ArrayList<>();
}
