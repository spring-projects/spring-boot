/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest @RestClientTest} with two {@code RestClient} clients.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
@RestClientTest({ ExampleRestClientService.class, AnotherExampleRestClientService.class })
class RestClientTestRestClientTwoComponentsIntegrationTests {

	@Autowired
	private ExampleRestClientService client1;

	@Autowired
	private AnotherExampleRestClientService client2;

	@Autowired
	private MockServerRestClientCustomizer customizer;

	@Autowired
	private MockRestServiceServer server;

	@Test
	void serverShouldNotWork() {
		assertThatIllegalStateException().isThrownBy(
				() -> this.server.expect(requestTo(uri("/test"))).andRespond(withSuccess("hello", MediaType.TEXT_HTML)))
			.withMessageContaining("Unable to use auto-configured");
	}

	@Test
	void client1RestCallViaCustomizer() {
		MockRestServiceServer server = this.customizer.getServer(this.client1.getRestClientBuilder());
		assertThat(server).isNotNull();
		server.expect(requestTo(uri("/test"))).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(this.client1.test()).isEqualTo("hello");
	}

	@Test
	void client2RestCallViaCustomizer() {
		MockRestServiceServer server = this.customizer.getServer(this.client2.getRestClientBuilder());
		assertThat(server).isNotNull();
		server.expect(requestTo(uri("/test"))).andRespond(withSuccess("there", MediaType.TEXT_HTML));
		assertThat(this.client2.test()).isEqualTo("there");
	}

	private static String uri(String path) {
		return "https://example.com" + path;
	}

}
