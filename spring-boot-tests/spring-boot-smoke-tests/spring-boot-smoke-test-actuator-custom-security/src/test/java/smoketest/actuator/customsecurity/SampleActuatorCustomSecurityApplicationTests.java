/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.actuator.customsecurity;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for actuator endpoints with custom security configuration.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "server.error.include-message=always" })
class SampleActuatorCustomSecurityApplicationTests extends AbstractSampleActuatorCustomSecurityTests {

	@LocalServerPort
	private int port;

	@Autowired
	private Environment environment;

	@Override
	String getPath() {
		return "http://localhost:" + this.port;
	}

	@Override
	String getManagementPath() {
		return "http://localhost:" + this.port;
	}

	@Override
	Environment getEnvironment() {
		return this.environment;
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void testInsecureApplicationPath() {
		ResponseEntity<Map> entity = restTemplate().getForEntity(getPath() + "/foo", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		Map<String, Object> body = entity.getBody();
		assertThat((String) body.get("message")).contains("Expected exception in controller");
	}

	@Test
	void mvcMatchersCanBeUsedToSecureActuators() {
		ResponseEntity<Object> entity = beansRestTemplate().getForEntity(getManagementPath() + "/actuator/beans",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = beansRestTemplate().getForEntity(getManagementPath() + "/actuator/beans/", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

}
