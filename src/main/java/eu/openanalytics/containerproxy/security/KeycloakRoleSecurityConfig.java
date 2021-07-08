/**
 * ContainerProxy
 *
 * Copyright (C) 2016-2021 Open Analytics
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */

package eu.openanalytics.containerproxy.security;

import com.google.common.base.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

import javax.inject.Inject;

@Configuration
@ConditionalOnProperty(name="proxy.authentication", havingValue="keycloak")
public class KeycloakRoleSecurityConfig implements ICustomSecurityConfig{
    private final Logger log = LogManager.getLogger(getClass());

    @Inject
    Environment environment;

    @Override
    public void apply(WebSecurity web) throws Exception{
        ICustomSecurityConfig.super.apply(web);
    }

    @Override
    public void apply(HttpSecurity http) throws Exception {
        ICustomSecurityConfig.super.apply(http);
        String[] uriArray = new String[] {"/api/**", "/app/**", "/app_direct/**", "/filebrowser", "/controlpanel", environment.getProperty("proxy.landing-page")};
        String role = environment.getProperty("proxy.allowed-role");
        if (!Strings.isNullOrEmpty(role)){
            log.info("Enable allowed roles mode");
            http.authorizeRequests().antMatchers("/").anonymous();
            http.authorizeRequests().antMatchers(uriArray).hasAnyRole("ADMINS", role.toUpperCase());
        }
    }

}
