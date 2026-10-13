/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.prometheus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SamplePrometheusApplication}.
 *
 * @author Moritz Halbritter
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMetrics
@AutoConfigureTestRestTemplate
class SamplePrometheusApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void shouldExportExemplars() {
		for (int i = 0; i < 10; i++) {
			ResponseEntity<String> response = this.restTemplate.getForEntity("/actuator", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ACCEPT, "application/openmetrics-text; version=1.0.0; charset=utf-8");
		ResponseEntity<String> metrics = this.restTemplate.exchange("/actuator/prometheus", HttpMethod.GET,
				new HttpEntity<>(headers), String.class);
		assertThat(metrics.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(metrics.getBody()).containsSubsequence("http_client_requests_seconds_count", "span_id", "trace_id");
	}

}
