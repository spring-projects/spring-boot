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

package smoketest.session.redis;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleSessionRedisApplication}.
 *
 * @author Angel L. Villalain
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
public class SampleSessionRedisApplicationTests {

	@Container
	static RedisContainer redis = new RedisContainer();

	@Autowired
	private TestRestTemplate restTemplate;

	@DynamicPropertySource
	static void applicationProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", redis::getFirstMappedPort);
	}

	@Test
	@SuppressWarnings("unchecked")
	void sessionsEndpointShouldReturnUserSessions() {
		performLogin();
		ResponseEntity<Map<String, Object>> response = this.getSessions();
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Map<String, Object>> sessions = (List<Map<String, Object>>) response.getBody().get("sessions");
		assertThat(sessions.size()).isEqualTo(1);
	}

	private String performLogin() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "user");
		form.set("password", "password");
		ResponseEntity<String> entity = this.restTemplate.exchange("/login", HttpMethod.POST,
				new HttpEntity<>(form, headers), String.class);
		return entity.getHeaders().getFirst("Set-Cookie");
	}

	private RequestEntity<Object> getRequestEntity(URI uri) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth("user", "password");
		return new RequestEntity<>(headers, HttpMethod.GET, uri);
	}

	private ResponseEntity<Map<String, Object>> getSessions() {
		RequestEntity<Object> request = getRequestEntity(URI.create("/actuator/sessions?username=user"));
		ParameterizedTypeReference<Map<String, Object>> stringObjectMap = new ParameterizedTypeReference<Map<String, Object>>() {
		};
		return this.restTemplate.exchange(request, stringObjectMap);
	}

}
