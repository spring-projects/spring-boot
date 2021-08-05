/*
 * Copyright 2012-2021 the original author or authors.
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

package smoketest.secure.jersey;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

	@SuppressWarnings("deprecation")
	@Bean
	public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
		return new InMemoryUserDetailsManager(
				User.withDefaultPasswordEncoder().username("user").password("password").authorities("ROLE_USER")
						.build(),
				User.withDefaultPasswordEncoder().username("admin").password("admin")
						.authorities("ROLE_ACTUATOR", "ROLE_USER").build());
	}

	@Bean
	SecurityFilterChain configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http.authorizeRequests()
				.requestMatchers(EndpointRequest.to("health")).permitAll()
				.requestMatchers(EndpointRequest.toAnyEndpoint().excluding(MappingsEndpoint.class)).hasRole("ACTUATOR")
				.antMatchers("/**").hasRole("USER")
				.and()
			.httpBasic();
		return http.build();
		// @formatter:on
	}

}
