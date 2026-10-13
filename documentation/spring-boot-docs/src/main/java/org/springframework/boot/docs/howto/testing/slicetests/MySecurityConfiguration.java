/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.testing.slicetests;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class MySecurityConfiguration {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
		return http.build();
	}

}
