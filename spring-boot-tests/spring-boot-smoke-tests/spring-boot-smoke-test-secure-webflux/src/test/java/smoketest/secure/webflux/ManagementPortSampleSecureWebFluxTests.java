/*
 * Copyright 2012-2022 the original author or authors.
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
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "management.server.port=0" }, classes = {
		ManagementPortSampleSecureWebFluxTests.SecurityConfiguration.class, SampleSecureWebFluxApplication.class })
class ManagementPortSampleSecureWebFluxTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Autowired
	private WebTestClient webClient;

	@Test
	void testHome() {
		this.webClient.get().uri("http://localhost:" + this.port, String.class).header("Authorization", getBasicAuth())
				.exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("Hello user");
	}

	@Test
	void actuatorPathOnMainPortShouldNotMatch() {
		this.webClient.get().uri("http://localhost:" + this.port + "/actuator", String.class).exchange().expectStatus()
				.isUnauthorized();
		this.webClient.get().uri("http://localhost:" + this.port + "/actuator/health", String.class).exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void testSecureActuator() {
		this.webClient.get().uri("http://localhost:" + this.managementPort + "/actuator/env", String.class).exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void testInsecureActuator() {
		String responseBody = this.webClient.get()
				.uri("http://localhost:" + this.managementPort + "/actuator/health", String.class).exchange()
				.expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
		assertThat(responseBody).contains("\"status\":\"UP\"");
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration {

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
			http.httpBasic();
			return http.build();
		}

	}

}
