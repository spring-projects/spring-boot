/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.security.enablehttps

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class MySecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		// Customize the application security ...
		http.redirectToHttps { }
		return http.build()
	}

}

