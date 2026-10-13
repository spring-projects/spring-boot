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
 * Integration tests for endpoints configuration.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.mvc.servlet.path=/spring" })
@AutoConfigureTestRestTemplate
class ServletPathSampleActuatorApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testErrorPath() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(
				this.restTemplate.withBasicAuth("user", "password").getForEntity("/spring/error", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(entity.getBody()).containsEntry("error", "None");
		assertThat(entity.getBody()).containsEntry("status", 999);
	}

	@Test
	void testHealth() {
		ResponseEntity<String> entity = this.restTemplate.withBasicAuth("user", "password")
			.getForEntity("/spring/actuator/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

	@Test
	void testHomeIsSecure() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(this.restTemplate.getForEntity("/spring/", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(entity.getHeaders().headerNames()).doesNotContain("Set-Cookie");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <K, V> ResponseEntity<Map<K, V>> asMapEntity(ResponseEntity<Map> entity) {
		return (ResponseEntity) entity;
	}

}
