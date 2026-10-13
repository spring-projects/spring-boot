/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jsp;

import java.net.URI;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for JSP application.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SampleWebJspApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testJspWithEl() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("/resources/text.txt");
	}

	@Test
	void customErrorPage() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, URI.create("/foo"));
		ResponseEntity<String> entity = this.restTemplate.exchange(request, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(entity.getBody()).contains("Something went wrong: 500 Internal Server Error");
	}

}
