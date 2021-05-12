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

import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.boot.autoconfigure.security.reactive.PathRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for a secure reactive application with custom security.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
		SampleSecureWebFluxCustomSecurityTests.SecurityConfiguration.class, SampleSecureWebFluxApplication.class })
class SampleSecureWebFluxCustomSecurityTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void userDefinedMappingsSecure() {
		this.webClient.get().uri("/").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void healthDoesNotRequireAuthentication() {
		this.webClient.get().uri("/actuator/health").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isOk();
	}

	@Test
	void actuatorsSecuredByRole() {
		this.webClient.get().uri("/actuator/env").accept(MediaType.APPLICATION_JSON)
				.header("Authorization", getBasicAuth()).exchange().expectStatus().isForbidden();
	}

	@Test
	void actuatorsAccessibleOnCorrectLogin() {
		this.webClient.get().uri("/actuator/env").accept(MediaType.APPLICATION_JSON)
				.header("Authorization", getBasicAuthForAdmin()).exchange().expectStatus().isOk();
	}

	@Test
	void actuatorExcludedFromEndpointRequestMatcher() {
		this.webClient.get().uri("/actuator/mappings").accept(MediaType.APPLICATION_JSON)
				.header("Authorization", getBasicAuth()).exchange().expectStatus().isOk();
	}

	@Test
	void staticResourceShouldBeAccessible() {
		this.webClient.get().uri("/css/bootstrap.min.css").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isOk();
	}

	@Test
	void actuatorLinksIsSecure() {
		this.webClient.get().uri("/actuator").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isUnauthorized();
		this.webClient.get().uri("/actuator").accept(MediaType.APPLICATION_JSON)
				.header("Authorization", getBasicAuthForAdmin()).exchange().expectStatus().isOk();
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

	private String getBasicAuthForAdmin() {
		return "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes());
	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@SuppressWarnings("deprecation")
		@Bean
		MapReactiveUserDetailsService userDetailsService() {
			return new MapReactiveUserDetailsService(
					User.withDefaultPasswordEncoder().username("user").password("password").authorities("ROLE_USER")
							.build(),
					User.withDefaultPasswordEncoder().username("admin").password("admin")
							.authorities("ROLE_ACTUATOR", "ROLE_USER").build());
		}

		@Bean
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
			http.authorizeExchange((exchanges) -> {
				exchanges.matchers(EndpointRequest.to("health")).permitAll();
				exchanges.matchers(EndpointRequest.toAnyEndpoint().excluding(MappingsEndpoint.class))
						.hasRole("ACTUATOR");
				exchanges.matchers(PathRequest.toStaticResources().atCommonLocations()).permitAll();
				exchanges.pathMatchers("/login").permitAll();
				exchanges.anyExchange().authenticated();
			});
			http.httpBasic(Customizer.withDefaults());
			return http.build();
		}

	}

}
