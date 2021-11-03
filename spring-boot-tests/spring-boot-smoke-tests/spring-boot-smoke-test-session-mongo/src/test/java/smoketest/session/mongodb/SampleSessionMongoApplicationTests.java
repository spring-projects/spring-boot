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

package smoketest.session.mongodb;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleSessionMongoApplication}.
 *
 * @author Angel L. Villalain
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
public class SampleSessionMongoApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Container
	static MongoDBContainer mongo = new MongoDBContainer(DockerImageNames.mongo()).withStartupAttempts(3)
			.withStartupTimeout(Duration.ofMinutes(2));

	@DynamicPropertySource
	static void applicationProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
	}

	@Test
	@SuppressWarnings("unchecked")
	void sessionsEndpointShouldReturnUserSessions() {
		createSession(URI.create("/"));
		ResponseEntity<Map<String, Object>> response = getSessions();
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Map<String, Object>> sessions = (List<Map<String, Object>>) response.getBody().get("sessions");
		assertThat(sessions.size()).isEqualTo(1);
	}

	private void createSession(URI uri) {
		RequestEntity<Object> request = getRequestEntity(uri);
		this.restTemplate.exchange(request, String.class);
	}

	private RequestEntity<Object> getRequestEntity(URI uri) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth("user", "password");
		return new RequestEntity<>(headers, HttpMethod.GET, uri);
	}

	@SuppressWarnings("unchecked")
	private ResponseEntity<Map<String, Object>> getSessions() {
		RequestEntity<Object> request = getRequestEntity(URI.create("/actuator/sessions?username=user"));
		return (ResponseEntity<Map<String, Object>>) (ResponseEntity) this.restTemplate.exchange(request, Map.class);
	}

}
