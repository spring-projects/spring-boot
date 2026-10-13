/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.mustache;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for Mustache application.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleWebMustacheApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testMustacheTemplate() {
		this.restTestClient.get()
			.uri("/")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("Hello, Andy"));
	}

	@Test
	void testMustacheErrorTemplate() {
		this.restTestClient.get()
			.uri("/does-not-exist")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("Something went wrong: 404 Not Found"));
	}

	@Test
	void test503HtmlResource() {
		this.restTestClient.get()
			.uri("/serviceUnavailable")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("I'm a 503"));
	}

	@Test
	void test5xxHtmlResource() {
		this.restTestClient.get()
			.uri("/bang")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("I'm a 5xx"));
	}

	@Test
	void test507Template() {
		this.restTestClient.get()
			.uri("/insufficientStorage")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INSUFFICIENT_STORAGE)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("I'm a 507"));
	}

}
