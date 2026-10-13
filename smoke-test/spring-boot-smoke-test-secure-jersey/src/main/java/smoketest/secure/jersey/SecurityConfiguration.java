/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure.jersey;

import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

	@Bean
	@SuppressWarnings("deprecation")
	public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
		return new InMemoryUserDetailsManager(
				User.withDefaultPasswordEncoder()
					.username("user")
					.password("password")
					.authorities("ROLE_USER")
					.build(),
				User.withDefaultPasswordEncoder()
					.username("admin")
					.password("admin")
					.authorities("ROLE_ACTUATOR", "ROLE_USER")
					.build());
	}

	@Bean
	SecurityFilterChain configure(HttpSecurity http) {
		http.authorizeHttpRequests((requests) -> {
			requests.requestMatchers(EndpointRequest.to("health")).permitAll();
			requests.requestMatchers(EndpointRequest.toAnyEndpoint().excluding(MappingsEndpoint.class))
				.hasRole("ACTUATOR");
			requests.requestMatchers("/**").hasRole("USER");
		});
		http.httpBasic(Customizer.withDefaults());
		return http.build();
	}

}
