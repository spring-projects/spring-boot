/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.server;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.ws.test.server.MockWebServiceClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockWebServiceClientAutoConfiguration}.
 *
 * @author Daniil Razorenov
 */
class MockWebServiceClientAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MockWebServiceClientAutoConfiguration.class));

	@Test
	void shouldRegisterMockWebServiceClient() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockWebServiceClient.class));
	}

	@Test
	void shouldNotRegisterMockWebServiceClientWhenItIsNotOnTheClasspath() {
		FilteredClassLoader classLoader = new FilteredClassLoader(MockWebServiceClient.class);

		this.contextRunner.withClassLoader(classLoader)
			.run((context) -> assertThat(context).doesNotHaveBean(MockWebServiceClient.class));
	}

}
