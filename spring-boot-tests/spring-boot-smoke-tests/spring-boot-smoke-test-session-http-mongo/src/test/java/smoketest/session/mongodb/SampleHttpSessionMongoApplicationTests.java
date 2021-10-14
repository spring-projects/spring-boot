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

package smoketest.session.mongodb;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.security.user.name=" + SampleHttpSessionMongoApplicationTests.USERNAME,
				"spring.security.user.password=" + SampleHttpSessionMongoApplicationTests.PASSWORD })
public class SampleHttpSessionMongoApplicationTests {

	static final String USERNAME = "user";
	static final String PASSWORD = "password";

	@Autowired
	private TestRestTemplate template;

	@LocalServerPort
	private int port;

	@Test
	@SuppressWarnings("unchecked")
	void sessionsEndpointShouldReturnUserSessions() {
		createSession();
		ResponseEntity<Map<String, Object>> response = this.getSessions();
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Map<String, Object>> sessions = (List<Map<String, Object>>) response.getBody().get("sessions");
		assertThat(sessions.size()).isEqualTo(1);
	}

	private void createSession() {
		URI uri = URI.create("http://localhost:" + this.port + "/");
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(USERNAME, PASSWORD);
		RequestEntity<Object> request = new RequestEntity<>(headers, HttpMethod.GET, uri);
		this.template.exchange(request, String.class);
	}

	@SuppressWarnings("unchecked")
	private ResponseEntity<Map<String, Object>> getSessions() {
		return (ResponseEntity<Map<String, Object>>) (ResponseEntity) this.template.withBasicAuth(USERNAME, PASSWORD)
				.getForEntity("/actuator/sessions?username=" + USERNAME, Map.class);
	}

}
