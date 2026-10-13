/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.traditional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleTraditionalApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testHomeJsp() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = entity.getBody();
		assertThat(body).contains("<html>").contains("<h1>Home</h1>");
	}

	@Test
	void testStaticPage() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/index.html", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = entity.getBody();
		assertThat(body).contains("<html>").contains("<h1>Hello</h1>");
	}

}
