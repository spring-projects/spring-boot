/*
 * Copyright 2012-2021 the original author or authors.
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

package smoketest.actuator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for endpoints configuration.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.mvc.servlet.path=/spring" })
class ServletPathSampleActuatorApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testErrorPath() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(
				this.restTemplate.withBasicAuth("user", "password").getForEntity("/spring/error", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(entity.getBody().get("error")).isEqualTo("None");
		assertThat(entity.getBody().get("status")).isEqualTo(999);
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
		assertThat(entity.getHeaders()).doesNotContainKey("Set-Cookie");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <K, V> ResponseEntity<Map<K, V>> asMapEntity(ResponseEntity<Map> entity) {
		return (ResponseEntity) entity;
	}

}
