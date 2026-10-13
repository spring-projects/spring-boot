/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.humio;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.humio.HumioConfig;
import io.micrometer.humio.HumioMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HumioMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class HumioMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HumioMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HumioMeterRegistry.class));
	}

	@Test
	void autoConfiguresConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(HumioMeterRegistry.class)
				.hasSingleBean(HumioConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(HumioMeterRegistry.class)
				.doesNotHaveBean(HumioConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.humio.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(HumioMeterRegistry.class)
				.doesNotHaveBean(HumioConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(HumioMeterRegistry.class)
				.hasSingleBean(HumioConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(HumioMeterRegistry.class)
				.hasBean("customRegistry")
				.hasSingleBean(HumioConfig.class));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			HumioMeterRegistry registry = context.getBean(HumioMeterRegistry.class);
			new JvmMemoryMetrics().bindTo(registry);
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
		HumioConfig customConfig() {
			return (key) -> null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		HumioMeterRegistry customRegistry(HumioConfig config, Clock clock) {
			return new HumioMeterRegistry(config, clock);
		}

	}

}
