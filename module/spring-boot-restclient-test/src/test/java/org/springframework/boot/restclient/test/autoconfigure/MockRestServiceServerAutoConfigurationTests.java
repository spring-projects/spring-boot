/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.restclient.test.MockServerRestTemplateCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestExpectationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockRestServiceServerAutoConfiguration}.
 *
 * @author HuitaePark
 * @author Andy Wilkinson
 */
class MockRestServiceServerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.test.restclient.mockrestserviceserver.enabled=true")
		.withConfiguration(AutoConfigurations.of(MockRestServiceServerAutoConfiguration.class));

	@Test
	void registersMockServerRestClientCustomizer() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockServerRestClientCustomizer.class));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	@SuppressWarnings("removal")
	void registersMockServerRestTemplateCustomizer() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockServerRestTemplateCustomizer.class));
	}

	@Test
	void registersMockRestServiceServer() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockRestServiceServer.class));
	}

	@Test
	void backsOffWhenUserProvidesMockServerRestClientCustomizer() {
		this.contextRunner.withBean("userMockServerRestClientCustomizer", MockServerRestClientCustomizer.class)
			.run((context) -> assertThat(context).hasSingleBean(MockServerRestClientCustomizer.class)
				.hasBean("userMockServerRestClientCustomizer"));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	@SuppressWarnings("removal")
	void backsOffWhenUserProvidesMockServerRestTemplateCustomizer() {
		this.contextRunner.withBean("userMockServerRestTemplateCustomizer", MockServerRestTemplateCustomizer.class)
			.run((context) -> assertThat(context).hasSingleBean(MockServerRestTemplateCustomizer.class)
				.hasBean("userMockServerRestTemplateCustomizer"));
	}

	@Test
	void backsOffWhenUserProvidesMockRestServiceServer() {
		this.contextRunner
			.withBean("userMockRestServiceServer", MockRestServiceServer.class, mock(RequestExpectationManager.class))
			.run((context) -> assertThat(context).hasSingleBean(MockRestServiceServer.class)
				.hasBean("userMockRestServiceServer"));
	}

}
