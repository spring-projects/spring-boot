/*
 * Copyright 2013-2021 the original author or authors.
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

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class TracingEnvironmentPostProcessorTests {

	MockEnvironment mockEnvironment = new MockEnvironment();

	@Test
	void shouldDoNothingWhenTracingDisabled() {
		this.mockEnvironment.setProperty("management.tracing.enabled", "false");

		new TracingEnvironmentPostProcessor().postProcessEnvironment(this.mockEnvironment, null);

		assertThat(this.mockEnvironment.getProperty("logging.pattern.level")).isNullOrEmpty();
	}

	@Test
	void shouldSetTraceIdPatternWhenTracingEnabled() {
		this.mockEnvironment.setProperty("management.tracing.enabled", "true");

		new TracingEnvironmentPostProcessor().postProcessEnvironment(this.mockEnvironment, null);

		assertThat(this.mockEnvironment.getProperty("logging.pattern.level")).contains("trace");
	}

	@Test
	void shouldNotSetLoggingPatternLevelWhenConfigIsDisabled() {
		this.mockEnvironment.setProperty("management.tracing.enabled", "true");
		this.mockEnvironment.setProperty("management.tracing.default-logging-pattern-enabled", "false");

		new TracingEnvironmentPostProcessor().postProcessEnvironment(this.mockEnvironment, null);

		assertThat(this.mockEnvironment.getProperty("logging.pattern.level")).isNullOrEmpty();
	}

}
