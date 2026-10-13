/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure.jersey;

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
 * Integration tests for separate management and main service ports with custom management
 * context path.
 *
 * @author Dave Syer
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.server.base-path=/management" })
class ManagementPortAndPathJerseyApplicationTests extends AbstractJerseySecureTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Test
	void testMissing() {
		ResponseEntity<String> entity = new TestRestTemplate("admin", "admin")
			.getForEntity("http://localhost:" + this.managementPort + "/management/actuator/missing", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Override
	String getPath() {
		return "http://localhost:" + this.port;
	}

	@Override
	String getManagementPath() {
		return "http://localhost:" + this.managementPort + "/management";
	}

}
