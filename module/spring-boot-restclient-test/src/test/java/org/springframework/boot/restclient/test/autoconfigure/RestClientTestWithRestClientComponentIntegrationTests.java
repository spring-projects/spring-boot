/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest @RestClientTest} with a single client using
 * {@code RestClient}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
@RestClientTest(ExampleRestClientService.class)
class RestClientTestWithRestClientComponentIntegrationTests {

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ExampleRestClientService client;

	@Test
	void mockServerCall() {
		this.server.expect(requestTo("https://example.com/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(this.client.test()).isEqualTo("hello");
	}

}
