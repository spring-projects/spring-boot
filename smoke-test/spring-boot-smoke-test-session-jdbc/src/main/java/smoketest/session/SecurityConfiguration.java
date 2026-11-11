/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

	@Bean
	SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) {
		http.authorizeHttpRequests((requests) -> {
			requests.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll();
			requests.anyRequest().authenticated();
		});
		http.formLogin(withDefaults());
		http.httpBasic(withDefaults());
		http.csrf(CsrfConfigurer::disable);
		return http.build();
	}

}
