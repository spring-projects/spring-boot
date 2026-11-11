/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.security.springwebflux

import org.springframework.boot.security.autoconfigure.web.reactive.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration(proxyBeanMethods = false)
class MyWebFluxSecurityConfiguration {

	@Bean
	fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		http.authorizeExchange { spec ->
			spec.matchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
			spec.pathMatchers("/foo", "/bar").authenticated()
		}
		http.formLogin(withDefaults())
		return http.build()
	}

}

