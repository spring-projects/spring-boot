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
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class MyRandomPortRestTestClientAssertJTests {

	@Test
	void exampleTest(@Autowired RestTestClient restClient) {
		// @formatter:off
		ResponseSpec spec = restClient.get().uri("/").exchange();
		RestTestClientResponse response = RestTestClientResponse.from(spec);
		assertThat(response).hasStatusOk().bodyText().isEqualTo("Hello World");
		// @formatter:on
	}

}
