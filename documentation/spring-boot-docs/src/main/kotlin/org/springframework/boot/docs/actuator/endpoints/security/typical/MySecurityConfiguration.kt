/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.endpoints.security.typical

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration(proxyBeanMethods = false)
class MySecurityConfiguration {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http.securityMatcher(EndpointRequest.toAnyEndpoint()).authorizeHttpRequests { requests ->
			requests.anyRequest().hasRole("ENDPOINT_ADMIN")
		}
		http.httpBasic(withDefaults())
		return http.build()
	}

}

