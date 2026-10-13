/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.h2webconsole.springsecurity;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;

@Profile("dev")
@Configuration(proxyBeanMethods = false)
public class DevProfileSecurityConfiguration {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) {
		http.securityMatcher(PathRequest.toH2Console());
		http.authorizeHttpRequests(yourCustomAuthorization());
		http.csrf(CsrfConfigurer::disable);
		http.headers((headers) -> headers.frameOptions(FrameOptionsConfig::sameOrigin));
		return http.build();
	}

	// tag::customizer[]
	<T> Customizer<T> yourCustomAuthorization() {
		return (t) -> {
		};
	}
	// end::customizer[]

}
