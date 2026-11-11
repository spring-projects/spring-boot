/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.DefaultSecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	@Bean
	public DefaultSecurityFilterChain springWebFilterChain(HttpSecurity http) {
		return http.csrf(CsrfConfigurer::disable)
			// Demonstrate that method security works
			// Best practice to use both for defense in depth
			.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll())
			.httpBasic(withDefaults())
			.build();
	}

	@Bean
	@SuppressWarnings("deprecation")
	public InMemoryUserDetailsManager userDetailsService() {
		User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
		UserDetails rob = userBuilder.username("rob").password("rob").roles("USER").build();
		UserDetails admin = userBuilder.username("admin").password("admin").roles("USER", "ADMIN").build();
		return new InMemoryUserDetailsManager(rob, admin);
	}

}
