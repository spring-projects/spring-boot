/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.traditional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleTraditionalApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testHomeJsp() {
		this.restTestClient.get()
			.uri("/")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("<html>").contains("<h1>Home</h1>"));
	}

	@Test
	void testStaticPage() {
		this.restTestClient.get()
			.uri("/index.html")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("<html>").contains("<h1>Hello</h1>"));
	}

}
