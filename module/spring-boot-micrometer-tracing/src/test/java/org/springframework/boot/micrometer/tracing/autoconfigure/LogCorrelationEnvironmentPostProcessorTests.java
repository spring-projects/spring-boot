/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogCorrelationEnvironmentPostProcessor}.
 *
 * @author Jonatan Ivanov
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class LogCorrelationEnvironmentPostProcessorTests {

	private final ConfigurableEnvironment environment = new StandardEnvironment();

	private final SpringApplication application = new SpringApplication();

	private final LogCorrelationEnvironmentPostProcessor postProcessor = new LogCorrelationEnvironmentPostProcessor();

	@Test
	void getExpectCorrelationIdPropertyWhenMicrometerTracingPresentReturnsTrue() {
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, Boolean.class, false))
			.isTrue();
	}

	@Test
	@ClassPathExclusions("micrometer-tracing-*.jar")
	void getExpectCorrelationIdPropertyWhenMicrometerTracingMissingReturnsFalse() {
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, Boolean.class, false))
			.isFalse();
	}

	@Test
	void getExpectCorrelationIdPropertyWhenTracingDisabledReturnsFalse() {
		TestPropertyValues.of("management.tracing.export.enabled=false").applyTo(this.environment);
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, Boolean.class, false))
			.isFalse();
	}

	@Test
	void postProcessEnvironmentAddsEnumerablePropertySource() {
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		PropertySource<?> propertySource = this.environment.getPropertySources().get("logCorrelation");
		assertThat(propertySource).isInstanceOf(EnumerablePropertySource.class);
		assertThat(((EnumerablePropertySource<?>) propertySource).getPropertyNames())
			.containsExactly(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "logging.pattern.correlation");
	}

	@Test
	void getCorrelationPatternWhenMdcKeysAreDefaultReturnsPatternUsingDefaultKeys() {
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("logging.pattern.correlation"))
			.isEqualTo("%correlationId{traceId(32),spanId(16)}");
	}

	@Test
	void getCorrelationPatternWhenMdcKeysAreCustomizedReturnsPatternUsingThoseKeys() {
		TestPropertyValues
			.of("management.tracing.mdc.trace-id-key=customTraceId", "management.tracing.mdc.span-id-key=customSpanId")
			.applyTo(this.environment);
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("logging.pattern.correlation"))
			.isEqualTo("%correlationId{customTraceId(32),customSpanId(16)}");
	}

	@Test
	void getCorrelationPatternWhenSetByUserDoesNotOverride() {
		TestPropertyValues
			.of("management.tracing.mdc.trace-id-key=customTraceId", "logging.pattern.correlation=%correlationId{x(1)}")
			.applyTo(this.environment);
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("logging.pattern.correlation")).isEqualTo("%correlationId{x(1)}");
	}

	@Test
	void getCorrelationPatternWhenTracingDisabledReturnsNull() {
		TestPropertyValues
			.of("management.tracing.export.enabled=false", "management.tracing.mdc.trace-id-key=customTraceId")
			.applyTo(this.environment);
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("logging.pattern.correlation")).isNull();
	}

}
