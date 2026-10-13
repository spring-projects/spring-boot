/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.h2webconsole.springsecurity

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Profile("dev")
@Configuration(proxyBeanMethods = false)
class DevProfileSecurityConfiguration {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	fun h2ConsoleSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
		return http.authorizeHttpRequests(yourCustomAuthorization())
			.csrf { csrf -> csrf.disable() }
			.headers { headers -> headers.frameOptions { frameOptions -> frameOptions.sameOrigin() } }
			.build()
	}

	// tag::customizer[]
	private fun <T> yourCustomAuthorization(): Customizer<T> {
		return Customizer.withDefaults()
	}
	// end::customizer[]

}

