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

package smoketest.session.redis;

import java.util.List;
import java.util.Map;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleSessionRedisApplication}.
 *
 * @author Angel L. Villalain
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureRestTestClient
class SampleSessionRedisApplicationTests {

	@Container
	@ServiceConnection
	static RedisContainer redis = TestImage.container(RedisContainer.class);

	@Autowired
	private RestTestClient restTestClient;

	@Test
	@SuppressWarnings("unchecked")
	void sessionsEndpointShouldReturnUserSessions() {
		performLogin();
		Map<String, Object> body = getSessions().getResponseBody();
		assertThat(body).isNotNull();
		List<Map<String, Object>> sessions = (List<Map<String, Object>>) body.get("sessions");
		assertThat(sessions).hasSize(1);
	}

	private void performLogin() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "user");
		form.set("password", "password");
		this.restTestClient.post()
			.uri("/login")
			.accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.exchange();
	}

	private EntityExchangeResult<Map<String, Object>> getSessions() {
		ParameterizedTypeReference<Map<String, Object>> stringObjectMap = new ParameterizedTypeReference<>() {
		};
		return this.restTestClient.get()
			.uri("/actuator/sessions?username=user")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(stringObjectMap)
			.returnResult();
	}

}
