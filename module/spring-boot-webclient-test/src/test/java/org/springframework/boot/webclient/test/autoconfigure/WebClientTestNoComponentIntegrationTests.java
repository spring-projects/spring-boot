/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link WebClientTest @WebClientTest} with no specific client.
 *
 * @author Phillip Webb
 */
@WebClientTest
@Import(MockWebServerConfiguration.class)
class WebClientTestNoComponentIntegrationTests {

	@Autowired
	private MockWebServer server;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Test
	void exampleWebClientServiceIsNotInjected() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleWebClientService.class));
	}

	@Test
	void examplePropertiesIsNotInjected() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleProperties.class));
	}

	@Test
	void manuallyCreateBean() throws InterruptedException {
		ExampleWebClientService client = new ExampleWebClientService(this.webClientBuilder);
		this.server.enqueue(new MockResponse().setBody("hello"));
		assertThat(client.test()).isEqualTo("hello");
		this.server.takeRequest();
	}

}
