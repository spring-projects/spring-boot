/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.utilities.testresttemplate;

import org.junit.jupiter.api.Test;

import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MyTests {

	private final TestRestTemplate template = new TestRestTemplate();

	@Test
	void testRequest() {
		ResponseEntity<String> response = this.template.getForEntity("https://myhost.example.com/example",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		// Other assertions to verify the response
	}

}
