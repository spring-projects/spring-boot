/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SampleServletApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void testHomeIsSecure() {
		HttpStatusCode status = restClient().get()
			.uri("/")
			.accept(MediaType.APPLICATION_JSON)
			.exchange((request, response) -> response.getStatusCode());
		assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void testHome() {
		ResponseEntity<String> entity = restClient().get()
			.uri("/")
			.headers((headers) -> headers.setBasicAuth("user", getPassword()))
			.retrieve()
			.toEntity(String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	private RestClient restClient() {
		return RestClient.create("http://localhost:" + this.port);
	}

	private String getPassword() {
		return "password";
	}

}
