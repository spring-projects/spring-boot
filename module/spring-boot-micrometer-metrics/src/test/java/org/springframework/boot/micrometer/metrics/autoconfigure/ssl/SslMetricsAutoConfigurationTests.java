/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.ssl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslMetricsAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class SslMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SslMetricsAutoConfiguration.class));

	@Test
	void shouldSupplyMeterBinder() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
			.withBean(SimpleMeterRegistry.class)
			.run((context) -> assertThat(context).hasSingleBean(SslMeterBinder.class));
	}

	@Test
	void shouldBackOffIfSslBundlesIsMissing() {
		this.contextRunner.withBean(SimpleMeterRegistry.class)
			.run((context) -> assertThat(context).doesNotHaveBean(SslMeterBinder.class));
	}

	@Test
	void shouldBackOffIfMeterRegistryIsMissing() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SslMeterBinder.class));
	}

}
