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

package smoketest.actuator.customsecurity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * SecurityConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

	/**
	 * Creates an instance of InMemoryUserDetailsManager and populates it with a list of
	 * user details.
	 * @return the InMemoryUserDetailsManager instance
	 */
	@Bean
	public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
		List<UserDetails> userDetails = new ArrayList<>();
		userDetails.add(createUserDetails("user", "password", "ROLE_USER"));
		userDetails.add(createUserDetails("beans", "beans", "ROLE_BEANS"));
		userDetails.add(createUserDetails("admin", "admin", "ROLE_ACTUATOR", "ROLE_USER"));
		return new InMemoryUserDetailsManager(userDetails);
	}

	/**
	 * Creates a UserDetails object with the given username, password, and authorities.
	 * @param username the username for the user
	 * @param password the password for the user
	 * @param authorities the authorities for the user
	 * @return a UserDetails object representing the user
	 * @deprecated This method uses a deprecated method for password encoding. It is
	 * recommended to use a more secure method for password encoding.
	 */
	@SuppressWarnings("deprecation")
	private UserDetails createUserDetails(String username, String password, String... authorities) {
		UserBuilder builder = User.withDefaultPasswordEncoder();
		builder.username(username);
		builder.password(password);
		builder.authorities(authorities);
		return builder.build();
	}

	/**
	 * Configures the security filter chain for the application.
	 * @param http the HttpSecurity object to configure
	 * @param handlerMappingIntrospector the HandlerMappingIntrospector object to use for
	 * request matching
	 * @return the configured SecurityFilterChain
	 * @throws Exception if an error occurs during configuration
	 */
	@Bean
	SecurityFilterChain configure(HttpSecurity http, HandlerMappingIntrospector handlerMappingIntrospector)
			throws Exception {
		http.authorizeHttpRequests((requests) -> {
			requests.requestMatchers(new MvcRequestMatcher(handlerMappingIntrospector, "/actuator/beans"))
				.hasRole("BEANS");
			requests.requestMatchers(EndpointRequest.to("health")).permitAll();
			requests.requestMatchers(EndpointRequest.toAnyEndpoint().excluding(MappingsEndpoint.class))
				.hasRole("ACTUATOR");
			requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll();
			requests.requestMatchers(new AntPathRequestMatcher("/foo")).permitAll();
			requests.requestMatchers(new MvcRequestMatcher(handlerMappingIntrospector, "/error")).permitAll();
			requests.requestMatchers(new AntPathRequestMatcher("/**")).hasRole("USER");
		});
		http.cors(withDefaults());
		http.httpBasic(withDefaults());
		return http.build();
	}

}
