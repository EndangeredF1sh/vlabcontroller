package hk.edu.polyu.comp.vlabcontroller.repository;

import hk.edu.polyu.comp.vlabcontroller.entity.QUser;
import hk.edu.polyu.comp.vlabcontroller.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface UserRepository extends MongoRepository<User, String>, QuerydslPredicateExecutor<User> {
    default User findUserByIdOrCreate(String uid) {
        return this
            .findOne(QUser.user.id.eq(uid))
            .orElse(User.builder().id(uid).build());
    }
}
