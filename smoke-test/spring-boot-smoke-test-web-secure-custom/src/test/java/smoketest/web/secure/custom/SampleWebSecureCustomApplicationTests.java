/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.secure.custom;

import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 * @author Scott Frederick
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleWebSecureCustomApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@LocalServerPort
	private int port;

	@Test
	void testHome() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW)
			.exchange("/", HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		URI location = entity.getHeaders().getLocation();
		assertThat(location).isNotNull();
		assertThat(location.toString()).endsWith(this.port + "/login");
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
		URI location = entity.getHeaders().getLocation();
		assertThat(location).isNotNull();
		assertThat(location.toString()).endsWith(this.port + "/");
	}

}
