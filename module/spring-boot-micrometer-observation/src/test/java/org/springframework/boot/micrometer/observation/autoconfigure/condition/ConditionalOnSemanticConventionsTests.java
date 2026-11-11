/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnSemanticConventions}.
 *
 * @author Andy Wilkinson
 */
class ConditionalOnSemanticConventionsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(Micrometer.class, Otel.class);

	@Test
	void whenPropertyIsNotSetOnlyMicrometerIsActive() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(Micrometer.class).doesNotHaveBean(Otel.class));
	}

	@Test
	void whenPropertyIsSetToOpenTelemetryOnlyOpenTelementryIsActive() {
		this.contextRunner.withPropertyValues("management.observations.conventions=opentelemetry")
			.run((context) -> assertThat(context).hasSingleBean(Otel.class).doesNotHaveBean(Micrometer.class));
	}

	@Test
	void whenPropertyIsSetToMicrometerOnlyMicrometerIsActive() {
		this.contextRunner.withPropertyValues("management.observations.conventions=micrometer")
			.run((context) -> assertThat(context).hasSingleBean(Micrometer.class).doesNotHaveBean(Otel.class));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSemanticConventions(SemanticConventions.MICROMETER)
	static class Micrometer {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSemanticConventions(SemanticConventions.OPEN_TELEMETRY)
	static class Otel {

	}

}
