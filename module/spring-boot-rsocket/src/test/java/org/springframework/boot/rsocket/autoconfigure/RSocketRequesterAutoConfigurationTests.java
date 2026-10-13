/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.autoconfigure;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketConnectorConfigurer;
import org.springframework.messaging.rsocket.RSocketRequester;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RSocketRequesterAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Nguyen Bao Sach
 */
class RSocketRequesterAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(RSocketStrategiesAutoConfiguration.class, RSocketRequesterAutoConfiguration.class));

	@Test
	void shouldCreateBuilder() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(RSocketRequester.Builder.class));
	}

	@Test
	void shouldGetPrototypeScopedBean() {
		this.contextRunner.run((context) -> {
			RSocketRequester.Builder first = context.getBean(RSocketRequester.Builder.class);
			RSocketRequester.Builder second = context.getBean(RSocketRequester.Builder.class);
			assertThat(first).isNotEqualTo(second);
		});
	}

	@Test
	void shouldNotCreateBuilderIfAlreadyPresent() {
		this.contextRunner.withUserConfiguration(CustomRSocketRequesterBuilder.class).run((context) -> {
			RSocketRequester.Builder builder = context.getBean(RSocketRequester.Builder.class);
			assertThat(builder).isInstanceOf(MyRSocketRequesterBuilder.class);
		});
	}

	@Test
	void shouldCreateBuilderWithAvailableRSocketConnectorConfigurers() {
		RSocketConnectorConfigurer first = mock(RSocketConnectorConfigurer.class);
		RSocketConnectorConfigurer second = mock(RSocketConnectorConfigurer.class);
		this.contextRunner.withBean("first", RSocketConnectorConfigurer.class, () -> first)
			.withBean("second", RSocketConnectorConfigurer.class, () -> second)
			.run((context) -> {
				assertThat(context).getBeans(RSocketConnectorConfigurer.class).hasSize(2);
				RSocketRequester.Builder builder = context.getBean(RSocketRequester.Builder.class);
				assertThat(builder).extracting("rsocketConnectorConfigurers", as(InstanceOfAssertFactories.LIST))
					.containsExactly(first, second);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRSocketRequesterBuilder {

		@Bean
		MyRSocketRequesterBuilder myRSocketRequesterBuilder() {
			return mock(MyRSocketRequesterBuilder.class);
		}

	}

	interface MyRSocketRequesterBuilder extends RSocketRequester.Builder {

	}

}
