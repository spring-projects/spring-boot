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

package smoketest.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * SampleSessionWebFluxRedisApplication class.
 */
@SpringBootApplication
public class SampleSessionWebFluxRedisApplication {

	/**
	 * The main method is the entry point of the application. It starts the Spring
	 * application by running the SampleSessionWebFluxRedisApplication class.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(SampleSessionWebFluxRedisApplication.class);
	}

	/**
	 * Configures the security filter chain for the Spring WebFlux application.
	 * @param http the ServerHttpSecurity object used to configure the security settings
	 * @return the SecurityWebFilterChain object representing the configured security
	 * filter chain
	 */
	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http.authorizeExchange((exchange) -> exchange.anyExchange().authenticated());
		http.httpBasic((basic) -> basic.securityContextRepository(new WebSessionServerSecurityContextRepository()));
		http.formLogin(withDefaults());
		return http.build();
	}

}
