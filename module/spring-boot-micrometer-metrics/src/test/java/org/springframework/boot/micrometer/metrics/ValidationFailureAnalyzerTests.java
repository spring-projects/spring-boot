/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.newrelic.NewRelicConfig;
import io.micrometer.newrelic.NewRelicMeterRegistry;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link ValidationFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class ValidationFailureAnalyzerTests {

	@Test
	void analyzesMissingRequiredConfiguration() {
		FailureAnalysis analysis = new ValidationFailureAnalyzer()
			.analyze(createFailure(MissingAccountIdAndApiKeyConfiguration.class));
		assertThat(analysis).isNotNull();
		assertThat(analysis.getCause().getMessage()).contains("newrelic.metrics.export.apiKey was 'null'");
		assertThat(analysis.getDescription()).isEqualTo(String.format("Invalid Micrometer configuration detected:%n%n"
				+ "  - newrelic.metrics.export.apiKey was 'null' but it is required when publishing to Insights API%n"
				+ "  - newrelic.metrics.export.accountId was 'null' but it is required when publishing to Insights API"));
	}

	private Exception createFailure(Class<?> configuration) {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
			fail("Expected failure did not occur");
			throw new AssertionError("Should not be reached");
		}
		catch (Exception ex) {
			return ex;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class MissingAccountIdAndApiKeyConfiguration {

		@Bean
		NewRelicMeterRegistry meterRegistry() {
			return new NewRelicMeterRegistry(new NewRelicConfig() {

				@Override
				public @Nullable String get(String key) {
					return null;
				}

				@Override
				public String prefix() {
					return "newrelic.metrics.export";
				}

			}, Clock.SYSTEM);
		}

	}

}
