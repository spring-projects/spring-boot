/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.security.enablehttps;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class MySecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		// Customize the application security ...
		http.redirectToHttps(Customizer.withDefaults());
		return http.build();
	}

}
