/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import smoketest.actuator.ManagementPortSampleActuatorApplicationTests.CustomErrorAttributes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.endpoint.health.show-details=always" })
@Import(CustomErrorAttributes.class)
class ManagementPortSampleActuatorApplicationTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Autowired
	private CustomErrorAttributes errorAttributes;

	@Test
	@SuppressWarnings("unchecked")
	void testHome() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.port)
			.defaultHeaders((headers) -> headers.setBasicAuth("user", "password"))
			.build()
			.get()
			.uri("/")
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsEntry("message", "Hello Phil"));
	}

	@Test
	void testMetrics() {
		testHome(); // makes sure some requests have been made
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.build()
			.get()
			.uri("/actuator/metrics")
			.exchange()
			.expectStatus()
			.isUnauthorized();
	}

	@Test
	void testHealth() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.defaultHeaders((headers) -> headers.setBasicAuth("user", "password"))
			.build()
			.get()
			.uri("/actuator/health")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":\"UP\"")
				.contains("\"example\"")
				.contains("\"counter\":42"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testErrorPage() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.defaultHeaders((headers) -> headers.setBasicAuth("user", "password"))
			.build()
			.get()
			.uri("/error")
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsEntry("status", 999));
	}

	@Test
	@SuppressWarnings("unchecked")
	void securityContextIsAvailableToErrorHandling() {
		this.errorAttributes.securityContext = null;
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.defaultHeaders((headers) -> headers.setBasicAuth("user", "password"))
			.build()
			.get()
			.uri("/404")
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.NOT_FOUND)
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsEntry("status", 404));
		assertThat(this.errorAttributes.securityContext).isNotNull();
		assertThat(this.errorAttributes.securityContext.getAuthentication()).isNotNull();
	}

	static class CustomErrorAttributes extends DefaultErrorAttributes {

		private volatile @Nullable SecurityContext securityContext;

		@Override
		public Map<String, @Nullable Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
			this.securityContext = SecurityContextHolder.getContext();
			return super.getErrorAttributes(webRequest, options);
		}

	}

}
