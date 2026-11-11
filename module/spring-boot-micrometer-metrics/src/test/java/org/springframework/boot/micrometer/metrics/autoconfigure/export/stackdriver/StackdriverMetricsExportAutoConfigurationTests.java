/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.stackdriver;

import io.micrometer.core.instrument.Clock;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StackdriverMetricsExportAutoConfiguration}.
 *
 * @author Johannes Graf
 */
class StackdriverMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(StackdriverMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(StackdriverMeterRegistry.class));
	}

	@Test
	void failsWithoutAProjectId() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void autoConfiguresConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.stackdriver.metrics.export.project-id=test-project")
			.run((context) -> assertThat(context).hasSingleBean(StackdriverMeterRegistry.class)
				.hasSingleBean(StackdriverConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(StackdriverMeterRegistry.class)
				.doesNotHaveBean(StackdriverConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.stackdriver.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(StackdriverMeterRegistry.class)
				.doesNotHaveBean(StackdriverConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(StackdriverMeterRegistry.class)
				.hasSingleBean(StackdriverConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.withPropertyValues("management.stackdriver.metrics.export.project-id=test-project")
			.run((context) -> assertThat(context).hasSingleBean(StackdriverMeterRegistry.class)
				.hasBean("customRegistry")
				.hasSingleBean(StackdriverConfig.class));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.stackdriver.metrics.export.project-id=test-project")
			.run((context) -> {
				StackdriverMeterRegistry registry = context.getBean(StackdriverMeterRegistry.class);
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
		StackdriverConfig customConfig() {
			return (key) -> {
				if ("stackdriver.projectId".equals(key)) {
					return "test-project";
				}
				return null;
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		StackdriverMeterRegistry customRegistry(StackdriverConfig config, Clock clock) {
			return new StackdriverMeterRegistry(config, clock);
		}

	}

}
