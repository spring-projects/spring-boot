/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for
 * {@link ConditionalOnForwardedHeaderStrategy @ConditionalOnForwardedHeaderStrategy}.
 *
 * @author Phillip Webb
 */
class ConditionalOnForwardedHeaderStrategyTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(FrameworkRequiredConfiguration.class);

	@Test
	void matchesIfStrategyPropertyIsValue() {
		this.contextRunner.withPropertyValues("server.request.header.forwarded.strategy=framework")
			.run((context) -> assertThat(context).hasBean("bean"));
	}

	@Test
	void matchesIfDeprectedStrategyPropertyIsValue() {
		this.contextRunner.withPropertyValues("server.forward-headers-strategy=framework")
			.run((context) -> assertThat(context).hasBean("bean"));
	}

	@Test
	void doesNotMatchesIfStrategyIsNotValue() {
		this.contextRunner.withPropertyValues("server.request.header.forwarded.strategy=none")
			.run((context) -> assertThat(context).doesNotHaveBean("bean"));
	}

	@Test
	void doesNotMatchesIfStrategyIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("bean"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnForwardedHeaderStrategy(ForwardedHeaderStrategy.FRAMEWORK)
	static class FrameworkRequiredConfiguration {

		@Bean
		String bean() {
			return "bean";
		}

	}

}
