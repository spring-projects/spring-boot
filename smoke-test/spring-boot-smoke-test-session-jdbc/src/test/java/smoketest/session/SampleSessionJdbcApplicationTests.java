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

package smoketest.session;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleSessionJdbcApplication}.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "server.servlet.session.timeout:2", "debug=true" })
@AutoConfigureRestTestClient
class SampleSessionJdbcApplicationTests {

	private static final HttpClientSettings DONT_FOLLOW_REDIRECTS = HttpClientSettings.defaults()
		.withRedirects(HttpRedirects.DONT_FOLLOW);

	@Autowired
	private RestTestClient restTestClient;

	@LocalServerPort
	@SuppressWarnings("NullAway.Init")
	private String port;

	private static final URI ROOT_URI = URI.create("/");

	/**
	 * A client that follows redirects, used to emulate a browser session.
	 */
	private RestTestClient browserClient() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect()
			.build(HttpClientSettings.defaults());
		return RestTestClient.bindToServer(requestFactory).baseUrl("http://localhost:" + this.port).build();
	}

	@Test
	void sessionExpiry() throws Exception {
		String cookie = performLogin();
		String sessionId1 = performRequest(ROOT_URI, cookie).getResponseBody();
		String sessionId2 = performRequest(ROOT_URI, cookie).getResponseBody();
		assertThat(sessionId1).isEqualTo(sessionId2);
		Thread.sleep(2100);
		String loginPage = performRequest(ROOT_URI, cookie).getResponseBody();
		assertThat(loginPage).containsIgnoringCase("login");
	}

	private @Nullable String performLogin() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(DONT_FOLLOW_REDIRECTS);
		RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "user");
		form.set("password", "password");
		ResponseEntity<String> entity = restClient.post()
			.uri("http://localhost:" + this.port + "/login")
			.accept(MediaType.TEXT_HTML)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.toEntity(String.class);
		return entity.getHeaders().getFirst("Set-Cookie");
	}

	@Test
	@SuppressWarnings("unchecked")
	void sessionsEndpointShouldReturnUserSession() {
		performLogin();
		Map<String, Object> body = getSessions().getResponseBody();
		assertThat(body).isNotNull();
		List<Map<String, Object>> sessions = (List<Map<String, Object>>) body.get("sessions");
		assertThat(sessions).hasSize(1);
	}

	private EntityExchangeResult<String> performRequest(URI uri, @Nullable String cookie) {
		return browserClient().get()
			.uri(uri)
			.accept(MediaType.TEXT_HTML)
			.headers((headers) -> headers.addAll(getHeaders(cookie)))
			.exchangeSuccessfully()
			.expectBody(String.class)
			.returnResult();
	}

	private HttpHeaders getHeaders(@Nullable String cookie) {
		HttpHeaders headers = new HttpHeaders();
		if (cookie != null) {
			headers.set("Cookie", cookie);
		}
		else {
			headers.set("Authorization", getBasicAuth());
		}
		return headers;
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

	private EntityExchangeResult<Map<String, Object>> getSessions() {
		ParameterizedTypeReference<Map<String, Object>> stringObjectMap = new ParameterizedTypeReference<>() {
		};
		return this.restTestClient.get()
			.uri("/actuator/sessions?username=user")
			.headers((headers) -> headers.addAll(getHeaders(null)))
			.exchangeSuccessfully()
			.expectBody(stringObjectMap)
			.returnResult();
	}

}
