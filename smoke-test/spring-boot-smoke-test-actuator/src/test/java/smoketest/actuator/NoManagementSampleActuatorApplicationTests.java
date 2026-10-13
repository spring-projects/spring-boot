/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for switching off management endpoints.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "management.server.port=-1" })
@AutoConfigureTestRestTemplate
class NoManagementSampleActuatorApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testHome() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(
				this.restTemplate.withBasicAuth("user", "password").getForEntity("/", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).containsEntry("message", "Hello Phil");
	}

	@Test
	void testMetricsNotAvailable() {
		testHome(); // makes sure some requests have been made
		ResponseEntity<Map<String, Object>> entity = asMapEntity(
				this.restTemplate.withBasicAuth("user", "password").getForEntity("/metrics", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <K, V> ResponseEntity<Map<K, V>> asMapEntity(ResponseEntity<Map> entity) {
		return (ResponseEntity) entity;
	}

}
