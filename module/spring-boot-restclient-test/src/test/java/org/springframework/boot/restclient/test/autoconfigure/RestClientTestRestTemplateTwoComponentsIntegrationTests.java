/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestTemplateCustomizer;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest @RestClientTest} with two {@code RestTemplate} clients.
 *
 * @author Phillip Webb
 * @deprecated since 4.2.0 for removal in 4.4.0
 */
@RestClientTest({ ExampleRestTemplateService.class, AnotherExampleRestTemplateService.class })
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
class RestClientTestRestTemplateTwoComponentsIntegrationTests {

	@Autowired
	private ExampleRestTemplateService client1;

	@Autowired
	private AnotherExampleRestTemplateService client2;

	@Autowired
	private MockServerRestTemplateCustomizer customizer;

	@Autowired
	private MockRestServiceServer server;

	@Test
	void serverShouldNotWork() {
		assertThatIllegalStateException()
			.isThrownBy(
					() -> this.server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML)))
			.withMessageContaining("Unable to use auto-configured");
	}

	@Test
	void client1RestCallViaCustomizer() {
		MockRestServiceServer server = this.customizer.getServer(this.client1.getRestTemplate());
		assertThat(server).isNotNull();
		server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(this.client1.test()).isEqualTo("hello");
	}

	@Test
	void client2RestCallViaCustomizer() {
		MockRestServiceServer server = this.customizer.getServer(this.client2.getRestTemplate());
		assertThat(server).isNotNull();
		server.expect(requestTo("/test")).andRespond(withSuccess("there", MediaType.TEXT_HTML));
		assertThat(this.client2.test()).isEqualTo("there");
	}

}
