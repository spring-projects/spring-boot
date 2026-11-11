/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.prometheus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SamplePrometheusApplication}.
 *
 * @author Moritz Halbritter
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMetrics
class SamplePrometheusApplicationTests {

	@Autowired
	private RestClient.Builder restClientBuilder;

	@LocalServerPort
	private int port;

	@Test
	void shouldExportExemplars() {
		RestClient restClient = this.restClientBuilder.baseUrl("http://localhost:" + this.port).build();
		for (int i = 0; i < 10; i++) {
			ResponseEntity<Void> response = restClient.get().uri("/actuator").retrieve().toBodilessEntity();
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
		ResponseEntity<String> metrics = restClient.get()
			.uri("/actuator/prometheus")
			.header(HttpHeaders.ACCEPT, "application/openmetrics-text; version=1.0.0; charset=utf-8")
			.retrieve()
			.toEntity(String.class);
		assertThat(metrics.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(metrics.getBody()).containsSubsequence("http_client_requests_seconds_count", "span_id", "trace_id");
	}

}
