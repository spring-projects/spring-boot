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

package smoketest.secure.webflux;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Profile("custom-security")
@Configuration
public class CustomSecurityConfiguration {

	@Bean
	SecurityWebFilterChain security(ServerHttpSecurity http) {
		http.authorizeExchange((auth) -> auth.pathMatchers(HttpMethod.GET, "/api/resource").permitAll()
				.pathMatchers(HttpMethod.POST, "/api/resource").authenticated()).httpBasic(Customizer.withDefaults())
				.csrf().disable();

		return http.build();
	}

}
