/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.withrunningserver;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class MyRandomPortRestTestClientTests {

	@Test
	void exampleTest(@Autowired RestTestClient restClient) {
		// @formatter:off
		restClient
			.get().uri("/")
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class).isEqualTo("Hello World");
		// @formatter:on
	}

}
