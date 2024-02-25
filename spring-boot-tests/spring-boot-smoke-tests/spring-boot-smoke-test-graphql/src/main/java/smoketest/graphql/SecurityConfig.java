/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.DefaultSecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * SecurityConfig class.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	/**
     * Configures the security filter chain for the Spring Web application.
     * Disables CSRF protection and permits all requests.
     * Uses HTTP Basic authentication.
     * 
     * @param http the HttpSecurity object to configure
     * @return the configured DefaultSecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
	public DefaultSecurityFilterChain springWebFilterChain(HttpSecurity http) throws Exception {
		return http.csrf((csrf) -> csrf.disable())
			// Demonstrate that method security works
			// Best practice to use both for defense in depth
			.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll())
			.httpBasic(withDefaults())
			.build();
	}

	/**
     * Creates an in-memory user details manager with two users: "rob" and "admin".
     * The password encoder used is the default password encoder.
     * 
     * @return the in-memory user details manager
     */
    @Bean
	@SuppressWarnings("deprecation")
	public InMemoryUserDetailsManager userDetailsService() {
		User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
		UserDetails rob = userBuilder.username("rob").password("rob").roles("USER").build();
		UserDetails admin = userBuilder.username("admin").password("admin").roles("USER", "ADMIN").build();
		return new InMemoryUserDetailsManager(rob, admin);
	}

}
