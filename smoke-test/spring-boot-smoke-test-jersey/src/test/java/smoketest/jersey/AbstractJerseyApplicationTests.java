/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "logging.level.root=debug")
abstract class AbstractJerseyApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/hello", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void reverse() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/reverse?input=olleh", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("hello");
	}

	@Test
	void validation() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/reverse", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void actuatorStatus() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/actuator/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}");
	}

}
