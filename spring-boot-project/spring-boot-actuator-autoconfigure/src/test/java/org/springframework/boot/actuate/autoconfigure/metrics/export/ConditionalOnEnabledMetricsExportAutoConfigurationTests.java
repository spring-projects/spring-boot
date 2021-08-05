/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnEnabledMetricsExport}.
 *
 * @author Chris Bono
 */
class ConditionalOnEnabledMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple());

	@Test
	void exporterIsEnabledByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("simpleMeterRegistry"));
	}

	@Test
	void exporterCanBeSpecificallyDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.export.simple.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("simpleMeterRegistry"));
	}

	@Test
	void exporterCanBeGloballyDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.export.defaults.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean("simpleMeterRegistry"));
	}

	@Test
	void exporterCanBeGloballyDisabledWithSpecificOverride() {
		this.contextRunner
				.withPropertyValues("management.metrics.export.defaults.enabled=false",
						"management.metrics.export.simple.enabled=true")
				.run((context) -> assertThat(context).hasBean("simpleMeterRegistry"));
	}

}
