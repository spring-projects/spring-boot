/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.actuator.customsecurity;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SampleActuatorCustomSecurityApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	public void homeIsSecure() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = restTemplate().getForEntity("/", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertThat(body.get("error")).isEqualTo("Unauthorized");
		assertThat(entity.getHeaders()).doesNotContainKey("Set-Cookie");
	}

	@Test
	public void testInsecureApplicationPath() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = restTemplate().getForEntity("/foo", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertThat((String) body.get("message"))
				.contains("Expected exception in controller");
	}

	@Test
	public void testInsecureStaticResources() {
		ResponseEntity<String> entity = restTemplate()
				.getForEntity("/css/bootstrap.min.css", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("body");
	}

	@Test
	public void actuatorInsecureEndpoint() {
		ResponseEntity<String> entity = restTemplate().getForEntity("/actuator/health",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

	@Test
	public void actuatorLinksIsSecure() {
		ResponseEntity<Object> entity = restTemplate().getForEntity("/actuator",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = adminRestTemplate().getForEntity("/actuator", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void actuatorSecureEndpointWithAnonymous() {
		ResponseEntity<Object> entity = restTemplate().getForEntity("/actuator/env",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void actuatorSecureEndpointWithUnauthorizedUser() {
		ResponseEntity<Object> entity = userRestTemplate().getForEntity("/actuator/env",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void actuatorSecureEndpointWithAuthorizedUser() {
		ResponseEntity<Object> entity = adminRestTemplate().getForEntity("/actuator/env",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void actuatorCustomMvcSecureEndpointWithAnonymous() {
		ResponseEntity<String> entity = restTemplate()
				.getForEntity("/actuator/example/echo?text={t}", String.class, "test");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void actuatorCustomMvcSecureEndpointWithUnauthorizedUser() {
		ResponseEntity<String> entity = userRestTemplate()
				.getForEntity("/actuator/example/echo?text={t}", String.class, "test");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void actuatorCustomMvcSecureEndpointWithAuthorizedUser() {
		ResponseEntity<String> entity = adminRestTemplate()
				.getForEntity("/actuator/example/echo?text={t}", String.class, "test");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("test");
		assertThat(entity.getHeaders().getFirst("echo")).isEqualTo("test");
	}

	@Test
	public void actuatorExcludedFromEndpointRequestMatcher() {
		ResponseEntity<Object> entity = userRestTemplate()
				.getForEntity("/actuator/mappings", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void mvcMatchersCanBeUsedToSecureActuators() {
		ResponseEntity<Object> entity = beansRestTemplate()
				.getForEntity("/actuator/beans", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = beansRestTemplate().getForEntity("/actuator/beans/", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	private TestRestTemplate restTemplate() {
		return configure(new TestRestTemplate());
	}

	private TestRestTemplate adminRestTemplate() {
		return configure(new TestRestTemplate("admin", "admin"));
	}

	private TestRestTemplate userRestTemplate() {
		return configure(new TestRestTemplate("user", "password"));
	}

	private TestRestTemplate beansRestTemplate() {
		return configure(new TestRestTemplate("beans", "beans"));
	}

	private TestRestTemplate configure(TestRestTemplate restTemplate) {
		restTemplate
				.setUriTemplateHandler(new LocalHostUriTemplateHandler(this.environment));
		return restTemplate;
	}

}
