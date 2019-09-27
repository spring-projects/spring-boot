/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.secure.jersey;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
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
		properties = { "management.server.port=0", "management.server.servlet.context-path=/management" })
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
