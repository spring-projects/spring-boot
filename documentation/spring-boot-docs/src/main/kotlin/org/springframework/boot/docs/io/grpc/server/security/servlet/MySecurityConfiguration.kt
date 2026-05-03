package org.springframework.boot.docs.io.grpc.server.security.servlet

import org.springframework.boot.grpc.server.autoconfigure.security.web.servlet.GrpcRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration(proxyBeanMethods = false)
class MySecurityConfiguration {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http.securityMatcher(GrpcRequest.toAnyService().excluding("special"))
		http.authorizeHttpRequests { requests -> requests.anyRequest().hasRole("GRPC_ADMIN") }
		http.httpBasic(withDefaults())
		return http.build()
	}

}
