/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.PingHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnEnabledHealthIndicator}.
 *
 * @author Phillip Webb
 */
class ConditionalOnEnabledHealthIndicatorTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(CustomHealthIndicatorConfiguration.class);

	@Test
	void whenNoPropertyCreatesBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(HealthIndicator.class));
	}

	@Test
	void whenIndicatorPropertyTrueCreatesBean() {
		this.contextRunner.withPropertyValues("management.health.custom.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(HealthIndicator.class));
	}

	@Test
	void whenIndicatorPropertyFalseDoesNotCreateBean() {
		this.contextRunner.withPropertyValues("management.health.custom.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class));
	}

	@Test
	void whenDefaultsPropertyTrueCreatesBean() {
		this.contextRunner.withPropertyValues("management.health.defaults.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(HealthIndicator.class));
	}

	@Test
	void whenDefaultsPropertyFalseDoesNotCreateBean() {
		this.contextRunner.withPropertyValues("management.health.defaults.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class));
	}

	@Test
	void whenIndicatorPropertyTrueAndDefaultsPropertyFalseCreatesBean() {
		this.contextRunner
			.withPropertyValues("management.health.custom.enabled=true", "management.health.defaults.enabled=false")
			.run((context) -> assertThat(context).hasSingleBean(HealthIndicator.class));
	}

	@Test
	void whenIndicatorPropertyFalseAndDefaultsPropertyTrueDoesNotCreateBean() {
		this.contextRunner
			.withPropertyValues("management.health.custom.enabled=false", "management.health.defaults.enabled=true")
			.run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnEnabledHealthIndicator("custom")
		PingHealthIndicator customHealthIndicator() {
			return new PingHealthIndicator();
		}

	}

}
