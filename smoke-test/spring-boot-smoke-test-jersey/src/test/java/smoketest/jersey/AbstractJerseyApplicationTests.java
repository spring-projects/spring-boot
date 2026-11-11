/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "logging.level.root=debug")
abstract class AbstractJerseyApplicationTests {

	private final RestClient restClient = RestClient.create();

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		ResponseEntity<String> entity = getForEntity("/hello");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void reverse() {
		ResponseEntity<String> entity = getForEntity("/reverse?input=olleh");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("hello");
	}

	@Test
	void validation() {
		ResponseEntity<String> entity = getForEntity("/reverse");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void actuatorStatus() {
		ResponseEntity<String> entity = getForEntity("/actuator/health");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}");
	}

	private ResponseEntity<String> getForEntity(String path) {
		String uri = LocalTestWebServer.obtain(this.applicationContext).uri(path);
		return this.restClient.get().uri(uri).retrieve().onStatus(HttpStatusCode::isError, (request, response) -> {
		}).toEntity(String.class);
	}

}
