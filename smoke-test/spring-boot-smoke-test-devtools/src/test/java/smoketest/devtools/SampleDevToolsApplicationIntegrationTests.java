/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.devtools;

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
 * Integration tests for {@link SampleDevToolsApplication}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleDevToolsApplicationIntegrationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testStaticResource() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/css/application.css", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("color: green;");
	}

	@Test
	void testPublicResource() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/public.txt", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("public file");
	}

	@Test
	void testClassResource() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/application.properties", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

}
