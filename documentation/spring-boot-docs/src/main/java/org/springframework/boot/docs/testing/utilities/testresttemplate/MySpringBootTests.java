/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.utilities.testresttemplate;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class MySpringBootTests {

	@Autowired
	private TestRestTemplate template;

	@Test
	void testRequest() {
		ResponseEntity<String> response = this.template.getForEntity("/example", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		// Other assertions to verify the response
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class RestTemplateBuilderConfiguration {

		@Bean
		RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().connectTimeout(Duration.ofSeconds(1)).readTimeout(Duration.ofSeconds(1));
		}

	}

}
