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
 * {@code RestTemplate}.
 *
 * @author Phillip Webb
 * @deprecated since 4.2.0 for removal in 4.4.0
 */
@RestClientTest(ExampleRestTemplateService.class)
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
class RestClientTestWithRestTemplateComponentIntegrationTests {

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ExampleRestTemplateService client;

	@Test
	void mockServerCall() {
		this.server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(this.client.test()).isEqualTo("hello");
	}

}
