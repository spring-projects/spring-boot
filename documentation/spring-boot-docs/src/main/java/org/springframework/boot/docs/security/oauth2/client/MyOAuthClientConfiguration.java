/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.security.oauth2.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class MyOAuthClientConfiguration {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		// @formatter:off
		http.authorizeHttpRequests((requests) ->
			requests.anyRequest().authenticated()
		);
		http.oauth2Login((login) ->
			login.redirectionEndpoint((endpoint) ->
				endpoint.baseUri("/login/oauth2/callback/*")
			)
		);
		// @formatter:on
		return http.build();
	}

}
