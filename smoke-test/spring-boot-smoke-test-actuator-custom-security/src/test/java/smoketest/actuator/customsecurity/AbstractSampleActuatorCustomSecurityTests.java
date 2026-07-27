/*
 * Copyright 2012-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for actuator tests with custom security.
 *
 * @author Madhura Bhave
 */
abstract class AbstractSampleActuatorCustomSecurityTests {

	abstract String getPath();

	abstract String getActuatorPath();

	abstract ApplicationContext getApplicationContext();

	@Test
	void homeIsSecure() {
		restTestClient().get()
			.uri(getPath() + "/")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isUnauthorized()
			.expectHeader()
			.doesNotExist("Set-Cookie");
	}

	@Test
	void testInsecureStaticResources() {
		restTestClient().get()
			.uri(getPath() + "/css/bootstrap.min.css")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("body"));
	}

	@Test
	void actuatorInsecureEndpoint() {
		RestTestClient restTestClient = restTestClient();
		restTestClient.get()
			.uri(getActuatorPath() + "/health")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":\"UP\""));
		restTestClient.get()
			.uri(getActuatorPath() + "/health/diskSpace")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":\"UP\""));
	}

	@Test
	void actuatorLinksWithAnonymous() {
		RestTestClient restTestClient = restTestClient();
		restTestClient.get().uri(getActuatorPath()).exchange().expectStatus().isUnauthorized();
		restTestClient.get().uri(getActuatorPath() + "/").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void actuatorLinksWithUnauthorizedUser() {
		RestTestClient restTestClient = userRestTestClient();
		restTestClient.get().uri(getActuatorPath()).exchange().expectStatus().isForbidden();
		restTestClient.get().uri(getActuatorPath() + "/").exchange().expectStatus().isForbidden();
	}

	@Test
	void actuatorLinksWithAuthorizedUser() {
		RestTestClient restTestClient = adminRestTestClient();
		restTestClient.get().uri(getActuatorPath()).accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk();
	}

	@Test
	void actuatorSecureEndpointWithAnonymous() {
		RestTestClient restTestClient = restTestClient();
		restTestClient.get().uri(getActuatorPath() + "/env").exchange().expectStatus().isUnauthorized();
		restTestClient.get()
			.uri(getActuatorPath() + "/env/management.endpoints.web.exposure.include")
			.exchange()
			.expectStatus()
			.isUnauthorized();
	}

	@Test
	void actuatorSecureEndpointWithUnauthorizedUser() {
		RestTestClient restTestClient = userRestTestClient();
		restTestClient.get().uri(getActuatorPath() + "/env").exchange().expectStatus().isForbidden();
		restTestClient.get()
			.uri(getActuatorPath() + "/env/management.endpoints.web.exposure.include")
			.exchange()
			.expectStatus()
			.isForbidden();
	}

	@Test
	void actuatorSecureEndpointWithAuthorizedUser() {
		RestTestClient restTestClient = adminRestTestClient();
		restTestClient.get().uri(getActuatorPath() + "/env").exchange().expectStatus().isOk();
		// EndpointRequest matches the trailing slash but MVC doesn't
		restTestClient.get().uri(getActuatorPath() + "/env/").exchange().expectStatus().isNotFound();
		restTestClient.get()
			.uri(getActuatorPath() + "/env/management.endpoints.web.exposure.include")
			.exchange()
			.expectStatus()
			.isOk();
	}

	@Test
	void secureServletEndpointWithAnonymous() {
		RestTestClient restTestClient = restTestClient();
		restTestClient.get().uri(getActuatorPath() + "/se1").exchange().expectStatus().isUnauthorized();
		restTestClient.get().uri(getActuatorPath() + "/se1/list").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void secureServletEndpointWithUnauthorizedUser() {
		RestTestClient restTestClient = userRestTestClient();
		restTestClient.get().uri(getActuatorPath() + "/se1").exchange().expectStatus().isForbidden();
		restTestClient.get().uri(getActuatorPath() + "/se1/list").exchange().expectStatus().isForbidden();
	}

	@Test
	void secureServletEndpointWithAuthorizedUser() {
		RestTestClient restTestClient = adminRestTestClient();
		restTestClient.get().uri(getActuatorPath() + "/se1").exchange().expectStatus().isOk();
		restTestClient.get().uri(getActuatorPath() + "/se1/list").exchange().expectStatus().isOk();
	}

	@Test
	void actuatorCustomMvcSecureEndpointWithAnonymous() {
		restTestClient().get()
			.uri(getActuatorPath() + "/example/echo?text={t}", "test")
			.exchange()
			.expectStatus()
			.isUnauthorized();
	}

	@Test
	void actuatorCustomMvcSecureEndpointWithUnauthorizedUser() {
		userRestTestClient().get()
			.uri(getActuatorPath() + "/example/echo?text={t}", "test")
			.exchange()
			.expectStatus()
			.isForbidden();
	}

	@Test
	void actuatorCustomMvcSecureEndpointWithAuthorizedUser() {
		adminRestTestClient().get()
			.uri(getActuatorPath() + "/example/echo?text={t}", "test")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.valueEquals("echo", "test")
			.expectBody(String.class)
			.isEqualTo("test");
	}

	@Test
	void actuatorExcludedFromEndpointRequestMatcher() {
		userRestTestClient().get().uri(getActuatorPath() + "/mappings").exchange().expectStatus().isOk();
	}

	RestTestClient restTestClient() {
		return configure(RestTestClient.bindToServer()).build();
	}

	RestTestClient adminRestTestClient() {
		return configure(
				RestTestClient.bindToServer().defaultHeaders((headers) -> headers.setBasicAuth("admin", "admin")))
			.build();
	}

	RestTestClient userRestTestClient() {
		return configure(
				RestTestClient.bindToServer().defaultHeaders((headers) -> headers.setBasicAuth("user", "password")))
			.build();
	}

	RestTestClient beansRestTestClient() {
		return configure(
				RestTestClient.bindToServer().defaultHeaders((headers) -> headers.setBasicAuth("beans", "beans")))
			.build();
	}

	RestTestClient.Builder<?> configure(RestTestClient.Builder<?> builder) {
		LocalTestWebServer localTestWebServer = LocalTestWebServer.obtain(getApplicationContext());
		return builder.baseUrl(localTestWebServer.uri());
	}

}
