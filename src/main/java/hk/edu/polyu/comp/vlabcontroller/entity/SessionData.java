package hk.edu.polyu.comp.vlabcontroller.entity;

import com.querydsl.core.annotations.QueryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@QueryEntity
public class SessionData {
    private DateTime loggedInAt;
    private DateTime loggedOutAt;
}
