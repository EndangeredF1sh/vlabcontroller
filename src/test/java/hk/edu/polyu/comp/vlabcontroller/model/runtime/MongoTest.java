package hk.edu.polyu.comp.vlabcontroller.model.runtime;

import hk.edu.polyu.comp.vlabcontroller.entity.QUser;
import hk.edu.polyu.comp.vlabcontroller.entity.User;
import hk.edu.polyu.comp.vlabcontroller.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.util.AssertionErrors.assertNotNull;

@SpringBootTest
public class MongoTest {
    @Autowired
    UserRepository repository;

    @Test
    public void testUserRepo() {
        this.repository.insert(User.builder().id("test").build());
        assertNotNull("entity is null", this.repository.findOne(QUser.user.id.eq("test")));
    }
}
