/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.statsd;

import io.micrometer.core.instrument.Clock;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StatsdMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class StatsdMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(StatsdMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(StatsdMeterRegistry.class));
	}

	@Test
	void autoConfiguresItsConfigMeterRegistryAndMetrics() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(StatsdMeterRegistry.class)
				.hasSingleBean(StatsdConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(StatsdMeterRegistry.class)
				.doesNotHaveBean(StatsdConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withPropertyValues("management.statsd.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(StatsdMeterRegistry.class)
				.doesNotHaveBean(StatsdConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(StatsdMeterRegistry.class)
				.hasSingleBean(StatsdConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(StatsdMeterRegistry.class)
				.hasBean("customRegistry")
				.hasSingleBean(StatsdConfig.class));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			StatsdMeterRegistry registry = context.getBean(StatsdMeterRegistry.class);
			assertThat(registry.isClosed()).isFalse();
			context.close();
			assertThat(registry.isClosed()).isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		Clock clock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		StatsdConfig customConfig() {
			return (key) -> null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		StatsdMeterRegistry customRegistry(StatsdConfig config, Clock clock) {
			return new StatsdMeterRegistry(config, clock);
		}

	}

}
