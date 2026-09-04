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

package smoketest.web.secure;

import java.net.URI;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 * @author Scott Frederick
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { SampleWebSecureApplicationTests.SecurityConfiguration.class, SampleWebSecureApplication.class })
@AutoConfigureRestTestClient
class SampleWebSecureApplicationTests {

	@Autowired
	private RestTestClient rest;

	@LocalServerPort
	private int port;

	private RestTestClient nonFollowingRedirect() {
		return RestTestClient
			.bindToServer(ClientHttpRequestFactoryBuilder.detect()
				.build(HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW)))
			.baseUrl("http://localhost:" + this.port)
			.build();
	}

	@Test
	void testHome() {
		EntityExchangeResult<String> result = nonFollowingRedirect().get()
			.uri("/home")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.returnResult(String.class);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.FOUND);
		URI location = result.getResponseHeaders().getLocation();
		assertThat(location).isNotNull();
		assertThat(location.toString()).endsWith(this.port + "/login");
	}

	@Test
	void testLoginPage() {
		this.rest.get()
			.uri("/login")
			.accept(MediaType.TEXT_HTML)
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("<title>Login</title>"));
	}

	@Test
	void testLogin() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "user");
		form.set("password", "password");
		EntityExchangeResult<String> result = nonFollowingRedirect().post()
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

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain configure(HttpSecurity http) {
			http.csrf(CsrfConfigurer::disable);
			http.authorizeHttpRequests((requests) -> {
				requests.requestMatchers("/public/**").permitAll();
				requests.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll();
				requests.anyRequest().fullyAuthenticated();
			});
			http.httpBasic(withDefaults());
			http.formLogin((form) -> form.loginPage("/login").permitAll());
			return http.build();
		}

	}

}
