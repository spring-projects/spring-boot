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

import java.util.Collections;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
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
@AutoConfigureTestRestTemplate
class SampleWebSecureApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@LocalServerPort
	private int port;

	@Test
	void testHome() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW)
			.exchange("/home", HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(entity.getHeaders().getLocation().toString()).endsWith(this.port + "/login");
	}

	@Test
	void testLoginPage() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restTemplate.exchange("/login", HttpMethod.GET, new HttpEntity<>(headers),
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("<title>Login</title>");
	}

	@Test
	void testLogin() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "user");
		form.set("password", "password");
		ResponseEntity<String> entity = this.restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW)
			.exchange("/login", HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(entity.getHeaders().getLocation().toString()).endsWith(this.port + "/");
	}

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain configure(HttpSecurity http) throws Exception {
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
