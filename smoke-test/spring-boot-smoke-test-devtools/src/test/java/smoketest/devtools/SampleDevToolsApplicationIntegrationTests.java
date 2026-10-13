/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.devtools;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SampleDevToolsApplication}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleDevToolsApplicationIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testStaticResource() {
		this.restTestClient.get()
			.uri("/css/application.css")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("color: green;"));
	}

	@Test
	void testPublicResource() {
		this.restTestClient.get()
			.uri("/public.txt")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("public file"));
	}

	@Test
	void testClassResource() {
		this.restTestClient.get().uri("/application.properties").exchange().expectStatus().isNotFound();
	}

}
