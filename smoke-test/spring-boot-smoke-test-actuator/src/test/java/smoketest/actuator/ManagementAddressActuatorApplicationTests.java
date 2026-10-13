/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "management.server.port=0",
		"management.server.address=127.0.0.1", "management.server.base-path:/admin" })
class ManagementAddressActuatorApplicationTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Test
	void testHome() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(
				new TestRestTemplate().getForEntity("http://localhost:" + this.port, Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void testHealth() {
		ResponseEntity<String> entity = new TestRestTemplate().withBasicAuth("user", "password")
			.getForEntity("http://localhost:" + this.managementPort + "/admin/actuator/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <K, V> ResponseEntity<Map<K, V>> asMapEntity(ResponseEntity<Map> entity) {
		return (ResponseEntity) entity;
	}

}
