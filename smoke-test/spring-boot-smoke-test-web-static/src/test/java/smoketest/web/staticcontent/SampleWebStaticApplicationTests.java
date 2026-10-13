/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.staticcontent;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleWebStaticApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testHome() {
		this.restTestClient.get()
			.uri("/")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("<title>Static"));
	}

	@Test
	void testCss() {
		this.restTestClient.get()
			.uri("/webjars/bootstrap/3.0.3/css/bootstrap.min.css")
			.exchangeSuccessfully()
			.expectHeader()
			.contentType(MediaType.valueOf("text/css"))
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("body"));
	}

}
