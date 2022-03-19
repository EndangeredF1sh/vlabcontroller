package hk.edu.polyu.comp.vlabcontroller.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "spring.session", name = "store-type", havingValue = "redis")
@RequiredArgsConstructor
//using @EnableRedisHttpSession which will result in Spring Boot's auto-configuration backing off.
public class RedisSessionHelper {
    final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public Map<String, ? extends Session> getSessionByUsername(String username) {
        return sessionRepository.findByIndexNameAndIndexValue(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, username);
    }
}
