/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.testng;

import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class SampleTestNGApplicationTests extends AbstractTestNGSpringContextTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testHome() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

}
