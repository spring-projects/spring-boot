/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.mustache;

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
 * Basic integration tests for Mustache application.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleWebMustacheApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testMustacheTemplate() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("Hello, Andy");
	}

	@Test
	void testMustacheErrorTemplate() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<String> responseEntity = this.restTemplate.exchange("/does-not-exist", HttpMethod.GET,
				requestEntity, String.class);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(responseEntity.getBody()).contains("Something went wrong: 404 Not Found");
	}

	@Test
	void test503HtmlResource() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange("/serviceUnavailable", HttpMethod.GET, requestEntity,
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(entity.getBody()).contains("I'm a 503");
	}

	@Test
	void test5xxHtmlResource() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange("/bang", HttpMethod.GET, requestEntity,
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(entity.getBody()).contains("I'm a 5xx");
	}

	@Test
	void test507Template() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange("/insufficientStorage", HttpMethod.GET,
				requestEntity, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_STORAGE);
		assertThat(entity.getBody()).contains("I'm a 507");
	}

}
