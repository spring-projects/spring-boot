/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.actuator.ui;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"management.server.port:0" })
public class SampleActuatorUiApplicationPortTests {

	@LocalServerPort
	private int port = 9010;

	@LocalManagementPort
	private int managementPort = 9011;

	@Test
	public void testHome() {
		ResponseEntity<String> entity = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void testMetrics() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.managementPort + "/actuator/metrics",
				Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void testHealth() {
		ResponseEntity<String> entity = new TestRestTemplate()
				.withBasicAuth("user", getPassword()).getForEntity(
						"http://localhost:" + this.managementPort + "/actuator/health",
						String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

	private String getPassword() {
		return "password";
	}

}
