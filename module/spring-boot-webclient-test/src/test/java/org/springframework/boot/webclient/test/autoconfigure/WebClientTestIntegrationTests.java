/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import java.nio.charset.StandardCharsets;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientTest @WebClientTest}.
 *
 * @author Scott Frederick
 */
@WebClientTest(ExampleWebClientService.class)
@Import(MockWebServerConfiguration.class)
class WebClientTestIntegrationTests {

	@Autowired
	private MockWebServer server;

	@Autowired
	private ExampleWebClientService client;

	@Test
	void mockServerCall1() throws InterruptedException {
		this.server.enqueue(new MockResponse().setBody("1"));
		assertThat(this.client.test()).isEqualTo("1");
		this.server.takeRequest();
	}

	@Test
	void mockServerCall2() throws InterruptedException {
		this.server.enqueue(new MockResponse().setBody("2"));
		assertThat(this.client.test()).isEqualTo("2");
		this.server.takeRequest();
	}

	@Test
	void mockServerCallWithContent() throws InterruptedException {
		this.server.enqueue(new MockResponse().setBody("1"));
		this.client.testPostWithBody("test");
		assertThat(this.server.takeRequest().getBody().readString(StandardCharsets.UTF_8)).isEqualTo("test");
	}

}
