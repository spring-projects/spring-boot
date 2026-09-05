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

package smoketest.security.method;

import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 * @author Scott Frederick
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.http.clients.imperative.factory=simple")
@AutoConfigureRestTestClient
class SampleMethodSecurityApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private RestTestClient rest;

	private RestTestClient followingRedirects() {
		return RestTestClient
			.bindToServer(ClientHttpRequestFactoryBuilder.detect()
				.build(HttpClientSettings.defaults().withRedirects(HttpRedirects.FOLLOW_WHEN_POSSIBLE)))
			.baseUrl("http://localhost:" + this.port)
			.build();
	}

	private RestTestClient nonFollowingRedirects() {
		return RestTestClient
			.bindToServer(ClientHttpRequestFactoryBuilder.detect()
				.build(HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW)))
			.baseUrl("http://localhost:" + this.port)
			.build();
	}

	@Test
	void testHome() {
		followingRedirects().get().uri("/").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk();
	}

	@Test
	void testLogin() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "admin");
		form.set("password", "admin");
		EntityExchangeResult<String> result = nonFollowingRedirects().post()
			.uri("/login")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.accept(MediaType.TEXT_HTML)
			.body(form)
			.exchange()
			.returnResult(String.class);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.FOUND);
		URI location = result.getResponseHeaders().getLocation();
		assertThat(location).isNotNull();
		assertThat(location.toString()).endsWith(this.port + "/");
	}

	@Test
	void testDenied() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "user");
		form.set("password", "user");
		EntityExchangeResult<String> result = nonFollowingRedirects().post()
			.uri("/login")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.accept(MediaType.TEXT_HTML)
			.body(form)
			.exchange()
			.returnResult(String.class);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.FOUND);
		String cookie = result.getResponseHeaders().getFirst("Set-Cookie");
		URI location = result.getResponseHeaders().getLocation();
		assertThat(location).isNotNull();
		EntityExchangeResult<String> page = this.rest.get().uri(location).headers((headers) -> {
			headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
			headers.set("Cookie", cookie);
		}).exchange().returnResult(String.class);
		assertThat(page.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(page.getResponseBody()).contains("Access denied");
	}

	@Test
	void testManagementProtected() {
		this.rest.get()
			.uri("/actuator/beans")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isUnauthorized();
	}

	@Test
	void testManagementAuthorizedAccess() {
		this.rest.get()
			.uri("/actuator/beans")
			.headers((headers) -> headers.setBasicAuth("admin", "admin"))
			.exchange()
			.expectStatus()
			.isOk();
	}

}
