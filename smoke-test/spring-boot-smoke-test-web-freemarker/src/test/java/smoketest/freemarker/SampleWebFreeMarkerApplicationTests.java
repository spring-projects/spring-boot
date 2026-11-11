/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.freemarker;

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
 * Basic integration tests for FreeMarker application.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleWebFreeMarkerApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testFreeMarkerTemplate() {
		this.restTestClient.get()
			.uri("/")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("Hello, Andy"));
	}

	@Test
	void testFreeMarkerErrorTemplate() {
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
	void templateErrorPageForSpecificStatusCode() {
		this.restTestClient.get()
			.uri("/insufficient-storage")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INSUFFICIENT_STORAGE)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("We are out of storage"));
	}

}
