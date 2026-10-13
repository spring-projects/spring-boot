/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.restclient.test.MockServerRestTemplateCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest @RestClientTest} with a {@code RestTemplate} and a
 * {@code RestClient} clients.
 *
 * @author Scott Frederick
 */
@RestClientTest({ ExampleRestTemplateService.class, ExampleRestClientService.class })
@SuppressWarnings("removal")
class RestClientTestRestTemplateAndRestClientTogetherIntegrationTests {

	@Autowired
	private ExampleRestTemplateService restTemplateClient;

	@Autowired
	private ExampleRestClientService restClientClient;

	@Autowired
	private MockServerRestTemplateCustomizer templateCustomizer;

	@Autowired
	private MockServerRestClientCustomizer clientCustomizer;

	@Autowired
	private MockRestServiceServer server;

	@Test
	void serverShouldNotWork() {
		assertThatIllegalStateException().isThrownBy(
				() -> this.server.expect(requestTo(uri("/test"))).andRespond(withSuccess("hello", MediaType.TEXT_HTML)))
			.withMessageContaining("Unable to use auto-configured");
	}

	@Test
	void restTemplateClientRestCallViaCustomizer() {
		this.templateCustomizer.getServer()
			.expect(requestTo("/test"))
			.andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(this.restTemplateClient.test()).isEqualTo("hello");
	}

	@Test
	void restClientClientRestCallViaCustomizer() {
		this.clientCustomizer.getServer()
			.expect(requestTo(uri("/test")))
			.andRespond(withSuccess("there", MediaType.TEXT_HTML));
		assertThat(this.restClientClient.test()).isEqualTo("there");
	}

	private static String uri(String path) {
		return "https://example.com" + path;
	}

}
