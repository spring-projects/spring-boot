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

package smoketest.actuator;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Basic integration tests for service demo application.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleActuatorApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testHomeIsSecure() {
		this.restTestClient.get()
			.uri("/")
			.exchange()
			.expectStatus()
			.isUnauthorized()
			.expectHeader()
			.doesNotExist("Set-Cookie");
	}

	@Test
	void testMetricsIsSecure() {
		this.restTestClient.get().uri("/actuator/metrics").exchange().expectStatus().isUnauthorized();
		this.restTestClient.get().uri("/actuator/metrics/").exchange().expectStatus().isUnauthorized();
		this.restTestClient.get().uri("/actuator/metrics/foo").exchange().expectStatus().isUnauthorized();
		this.restTestClient.get().uri("/actuator/metrics.json").exchange().expectStatus().isUnauthorized();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testHome() {
		this.restTestClient.get()
			.uri("/")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsEntry("message", "Hello Phil"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testMetrics() {
		testHome(); // makes sure some requests have been made
		this.restTestClient.get()
			.uri("/actuator/metrics")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> {
				assertThat(body).containsKey("names");
				List<String> names = (List<String>) body.get("names");
				assertThat(names).contains("jvm.buffer.count");
			});
	}

	@Test
	@SuppressWarnings("unchecked")
	void testEnv() {
		this.restTestClient.get()
			.uri("/actuator/env")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsKey("propertySources"));
	}

	@Test
	void healthInsecureByDefault() {
		this.restTestClient.get()
			.uri("/actuator/health")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":\"UP\"").doesNotContain("\"hello\":\"1\""));
	}

	@Test
	void testErrorPage() {
		this.restTestClient.get()
			.uri("/foo")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"error\":"));
	}

	@Test
	void testHtmlErrorPage() {
		this.restTestClient.get()
			.uri("/foo")
			.accept(MediaType.TEXT_HTML)
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(String.class)
			.value((body) -> assertThat(body).as("Body was null")
				.isNotNull()
				.contains("This application has no explicit mapping for /error"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testErrorPageDirectAccess() {
		this.restTestClient.get()
			.uri("/error")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(Map.class)
			.value((body) -> {
				assertThat(body).containsEntry("error", "None");
				assertThat(body).containsEntry("status", 999);
			});
	}

	@Test
	@SuppressWarnings("unchecked")
	void testBeans() {
		this.restTestClient.get()
			.uri("/actuator/beans")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsOnlyKeys("contexts"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testConfigProps() {
		this.restTestClient.get()
			.uri("/actuator/configprops")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> {
				Map<String, Object> contexts = (Map<String, Object>) body.get("contexts");
				assertThat(contexts).isNotNull();
				Map<String, Object> context = (Map<String, Object>) contexts.get(this.applicationContext.getId());
				assertThat(context).isNotNull();
				Map<String, Object> beans = (Map<String, Object>) context.get("beans");
				assertThat(beans).containsKey("spring.datasource-" + DataSourceProperties.class.getName());
			});
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLegacyDot() {
		this.restTestClient.get()
			.uri("/actuator/legacy")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).contains(entry("legacy", "legacy")));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLegacyHyphen() {
		this.restTestClient.get()
			.uri("/actuator/anotherlegacy")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).contains(entry("legacy", "legacy")));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testInfo() {
		this.restTestClient.get()
			.uri("/actuator/info")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> {
				assertThat(body).containsKey("build");
				Map<String, Object> example = (Map<String, Object>) body.get("example");
				assertThat(example).containsEntry("someKey", "someValue");
			});
	}

}
