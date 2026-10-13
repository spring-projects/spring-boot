/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest @RestClientTest} with a {@link RestClient}.
 *
 * @author Scott Frederick
 */
@RestClientTest(ExampleRestClientService.class)
class RestClientTestRestClientIntegrationTests {

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ExampleRestClientService client;

	@Test
	void mockServerCall1() {
		this.server.expect(requestTo(uri("/test"))).andRespond(withSuccess("1", MediaType.TEXT_HTML));
		assertThat(this.client.test()).isEqualTo("1");
	}

	@Test
	void mockServerCall2() {
		this.server.expect(requestTo(uri("/test"))).andRespond(withSuccess("2", MediaType.TEXT_HTML));
		assertThat(this.client.test()).isEqualTo("2");
	}

	@Test
	void mockServerCallWithContent() {
		this.server.expect(requestTo(uri("/test")))
			.andExpect(content().string("test"))
			.andRespond(withSuccess("1", MediaType.TEXT_HTML));
		this.client.testPostWithBody("test");
	}

	private static String uri(String path) {
		return "https://example.com" + path;
	}

}
