/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure.rsocket;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.rsocket.autoconfigure.RSocketRequesterAutoConfiguration;
import org.springframework.boot.rsocket.autoconfigure.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.RSocketGraphQlClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RSocketGraphQlClientAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class RSocketGraphQlClientAutoConfigurationTests {

	private static final RSocketGraphQlClient.Builder<?> builderInstance = RSocketGraphQlClient.builder();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RSocketStrategiesAutoConfiguration.class,
				RSocketRequesterAutoConfiguration.class, RSocketGraphQlClientAutoConfiguration.class));

	@Test
	void shouldCreateBuilder() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(RSocketGraphQlClient.Builder.class));
	}

	@Test
	void shouldGetPrototypeScopedBean() {
		this.contextRunner.run((context) -> {
			RSocketGraphQlClient.Builder<?> first = context.getBean(RSocketGraphQlClient.Builder.class);
			RSocketGraphQlClient.Builder<?> second = context.getBean(RSocketGraphQlClient.Builder.class);
			assertThat(first).isNotEqualTo(second);
		});
	}

	@Test
	void shouldNotCreateBuilderIfAlreadyPresent() {
		this.contextRunner.withUserConfiguration(CustomRSocketGraphQlClientBuilder.class).run((context) -> {
			RSocketGraphQlClient.Builder<?> builder = context.getBean(RSocketGraphQlClient.Builder.class);
			assertThat(builder).isEqualTo(builderInstance);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRSocketGraphQlClientBuilder {

		@Bean
		RSocketGraphQlClient.Builder<?> myRSocketGraphQlClientBuilder() {
			return builderInstance;
		}

	}

}
