/*
 * Copyright 2012-present the original author or authors.
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

package smoketest.session.hazelcast;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleSessionHazelcastApplication}.
 *
 * @author Susmitha Kandula
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleSessionHazelcastApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@SuppressWarnings("unchecked")
	void sessionsEndpointShouldReturnUserSession() {
		performLogin();
		ResponseEntity<Map<String, Object>> entity = getSessions();
		assertThat(entity).isNotNull();
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Map<String, Object>> sessions = (List<Map<String, Object>>) entity.getBody().get("sessions");
		assertThat(sessions).hasSize(1);
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

	private ResponseEntity<Map<String, Object>> getSessions() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", getBasicAuth());
		RequestEntity<Object> request = new RequestEntity<>(headers, HttpMethod.GET,
				URI.create("/actuator/sessions?username=user"));
		ParameterizedTypeReference<Map<String, Object>> stringObjectMap = new ParameterizedTypeReference<>() {
		};
		return this.restTemplate.exchange(request, stringObjectMap);
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
