package hk.edu.polyu.comp.vlabcontroller.entity;

import com.querydsl.core.annotations.QueryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@QueryEntity
@Document
public class User {
    @Id private String id;
    @Builder.Default private LinkedList<LabInstance> labs = new LinkedList<>();
    @Builder.Default private Map<String, SessionData> session = new HashMap<>();
}
