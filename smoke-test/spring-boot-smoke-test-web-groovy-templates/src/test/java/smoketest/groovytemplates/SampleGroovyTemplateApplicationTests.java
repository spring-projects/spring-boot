/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.groovytemplates;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.http.clients.redirects=dont-follow")
@AutoConfigureRestTestClient
class SampleGroovyTemplateApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testHome() {
		this.restTestClient.get().uri("/").exchangeSuccessfully().expectBody(String.class).value((body) -> {
			assertThat(body).contains("<title>Messages");
			assertThat(body).doesNotContain("layout:fragment");
		});
	}

	@Test
	void testCreate() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.set("text", "FOO text");
		map.set("summary", "FOO");
		this.restTestClient.post()
			.uri("/")
			.body(map)
			.exchange()
			.expectHeader()
			.value("Location", (location) -> assertThat(location).matches("/\\d+(;jsessionid=[\\w.]+)?"));
	}

	@Test
	void testCss() {
		this.restTestClient.get()
			.uri("/css/bootstrap.min.css")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("body"));
	}

}
