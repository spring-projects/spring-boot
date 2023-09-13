/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.tracing;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.core.env.ConfigurableEnvironment;
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
		TestPropertyValues.of("management.tracing.enabled=false").applyTo(this.environment);
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, Boolean.class, false))
			.isFalse();
	}

}
