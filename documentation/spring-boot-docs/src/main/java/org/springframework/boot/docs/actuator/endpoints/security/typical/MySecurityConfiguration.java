/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.endpoints.security.typical;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration(proxyBeanMethods = false)
public class MySecurityConfiguration {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http.securityMatcher(EndpointRequest.toAnyEndpoint());
		http.authorizeHttpRequests((requests) -> requests.anyRequest().hasRole("ENDPOINT_ADMIN"));
		http.httpBasic(withDefaults());
		return http.build();
	}

}
