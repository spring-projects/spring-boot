/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.freemarker;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for FreeMarker application.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleWebFreeMarkerApplicationTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	void testFreeMarkerTemplate() {
		ResponseEntity<String> entity = this.testRestTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("Hello, Andy");
	}

	@Test
	void testFreeMarkerErrorTemplate() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		ResponseEntity<String> responseEntity = this.testRestTemplate.exchange("/does-not-exist", HttpMethod.GET,
				requestEntity, String.class);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(responseEntity.getBody()).contains("Something went wrong: 404 Not Found");
	}

	@Test
	void templateErrorPageForSpecificStatusCode() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);

		ResponseEntity<String> responseEntity = this.testRestTemplate.exchange("/insufficient-storage", HttpMethod.GET,
				requestEntity, String.class);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_STORAGE);
		assertThat(responseEntity.getBody()).contains("We are out of storage");
	}

}
