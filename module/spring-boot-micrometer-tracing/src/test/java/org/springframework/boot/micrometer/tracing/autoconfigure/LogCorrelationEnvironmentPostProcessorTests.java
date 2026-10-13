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
			.containsExactly(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY);
	}

}
