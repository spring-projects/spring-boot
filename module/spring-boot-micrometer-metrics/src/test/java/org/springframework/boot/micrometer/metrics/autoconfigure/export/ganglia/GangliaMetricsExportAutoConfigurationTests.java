/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.ganglia;

import io.micrometer.core.instrument.Clock;
import io.micrometer.ganglia.GangliaConfig;
import io.micrometer.ganglia.GangliaMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GangliaMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class GangliaMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GangliaMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(GangliaMeterRegistry.class));
	}

	@Test
	void autoConfiguresItsConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(GangliaMeterRegistry.class)
				.hasSingleBean(GangliaConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GangliaMeterRegistry.class)
				.doesNotHaveBean(GangliaConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.ganglia.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GangliaMeterRegistry.class)
				.doesNotHaveBean(GangliaConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(GangliaMeterRegistry.class)
				.hasSingleBean(GangliaConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(GangliaMeterRegistry.class)
				.hasBean("customRegistry")
				.hasSingleBean(GangliaConfig.class));
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			GangliaMeterRegistry registry = context.getBean(GangliaMeterRegistry.class);
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
		GangliaConfig customConfig() {
			return (key) -> null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		GangliaMeterRegistry customRegistry(GangliaConfig config, Clock clock) {
			return new GangliaMeterRegistry(config, clock);
		}

	}

}
