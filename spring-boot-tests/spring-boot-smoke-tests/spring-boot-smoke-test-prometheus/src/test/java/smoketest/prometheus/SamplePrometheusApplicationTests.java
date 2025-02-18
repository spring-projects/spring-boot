/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.prometheus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
@AutoConfigureObservability
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
