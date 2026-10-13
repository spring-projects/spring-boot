/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.ui;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.web.error.include-message=always" })
@AutoConfigureTestRestTemplate
class SampleActuatorUiApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testHome() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restTemplate.withBasicAuth("user", getPassword())
			.exchange("/", HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("<title>Hello");
	}

	@Test
	void testCss() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/css/bootstrap.min.css", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("body");
	}

	@Test
	void testMetrics() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = this.restTemplate.getForEntity("/actuator/metrics", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void testError() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restTemplate.withBasicAuth("user", getPassword())
			.exchange("/error", HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(entity.getBody()).contains("<html>")
			.contains("<body>")
			.contains("Please contact the operator with the above information");
	}

	private String getPassword() {
		return "password";
	}

}
