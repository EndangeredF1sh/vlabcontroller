package hk.edu.polyu.comp.vlabcontroller.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

public interface ICustomSecurityConfig {

    default void apply(WebSecurity web) throws Exception {
    }

    default void apply(HttpSecurity http) throws Exception {
    }

}
