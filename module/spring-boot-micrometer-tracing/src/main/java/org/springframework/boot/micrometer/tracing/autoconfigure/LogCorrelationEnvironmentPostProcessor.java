/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.tracing.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties.Mdc;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} to add a {@link PropertySource} to support log
 * correlation IDs when Micrometer Tracing is present. Adds support for the
 * {@value LoggingSystem#EXPECT_CORRELATION_ID_PROPERTY} property by delegating to
 * {@code management.tracing.export.enabled}, and defaults
 * {@code logging.pattern.correlation} to the MDC keys configured through
 * {@code management.tracing.mdc.*}.
 *
 * @author Jonatan Ivanov
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class LogCorrelationEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (ClassUtils.isPresent("io.micrometer.tracing.Tracer", application.getClassLoader())) {
			environment.getPropertySources().addLast(new LogCorrelationPropertySource(this, environment));
		}
	}

	/**
	 * Log correlation {@link PropertySource}.
	 */
	private static class LogCorrelationPropertySource extends EnumerablePropertySource<Object> {

		private static final String NAME = "logCorrelation";

		private static final String CORRELATION_PATTERN_PROPERTY = "logging.pattern.correlation";

		/**
		 * Expected trace ID length, matching {@code CorrelationIdFormatter.DEFAULT}.
		 */
		private static final int TRACE_ID_LENGTH = 32;

		/**
		 * Expected span ID length, matching {@code CorrelationIdFormatter.DEFAULT}.
		 */
		private static final int SPAN_ID_LENGTH = 16;

		private final Environment environment;

		LogCorrelationPropertySource(Object source, Environment environment) {
			super(NAME, source);
			this.environment = environment;
		}

		@Override
		public String[] getPropertyNames() {
			return new String[] { LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, CORRELATION_PATTERN_PROPERTY };
		}

		@Override
		public @Nullable Object getProperty(String name) {
			if (name.equals(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY)) {
				return isExpectCorrelationId();
			}
			if (name.equals(CORRELATION_PATTERN_PROPERTY)) {
				return getCorrelationPattern();
			}
			return null;
		}

		private Boolean isExpectCorrelationId() {
			return this.environment.getProperty("management.tracing.export.enabled", Boolean.class, Boolean.TRUE);
		}

		private @Nullable String getCorrelationPattern() {
			if (!isExpectCorrelationId()) {
				return null;
			}
			String traceIdKey = this.environment.getProperty("management.tracing.mdc.trace-id-key",
					Mdc.DEFAULT_TRACE_ID_KEY);
			String spanIdKey = this.environment.getProperty("management.tracing.mdc.span-id-key",
					Mdc.DEFAULT_SPAN_ID_KEY);
			return "%%correlationId{%s(%d),%s(%d)}".formatted(traceIdKey, TRACE_ID_LENGTH, spanIdKey, SPAN_ID_LENGTH);
		}

	}

}
