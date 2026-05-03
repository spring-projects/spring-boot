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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OnEnabledTracingExportCondition}.
 *
 * @author Moritz Halbritter
 */
class OnEnabledTracingExportConditionTests {

	@Test
	void shouldMatchIfNoPropertyIsSet() {
		OnEnabledTracingExportCondition condition = new OnEnabledTracingExportCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(mockConditionContext(), mockMetadata(""));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).isEqualTo("@ConditionalOnEnabledTracingExport tracing is enabled by default");
	}

	@Test
	void shouldNotMatchIfGlobalPropertyIsFalse() {
		OnEnabledTracingExportCondition condition = new OnEnabledTracingExportCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(Map.of("management.tracing.export.enabled", "false")), mockMetadata(""));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnEnabledTracingExport management.tracing.export.enabled is false");
	}

	@Test
	void shouldMatchIfGlobalPropertyIsTrue() {
		OnEnabledTracingExportCondition condition = new OnEnabledTracingExportCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(Map.of("management.tracing.export.enabled", "true")), mockMetadata(""));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnEnabledTracingExport management.tracing.export.enabled is true");
	}

	@Test
	void shouldNotMatchIfExporterPropertyIsFalse() {
		OnEnabledTracingExportCondition condition = new OnEnabledTracingExportCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(Map.of("management.tracing.export.zipkin.enabled", "false")),
				mockMetadata("zipkin"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnEnabledTracingExport management.tracing.export.zipkin.enabled is false");
	}

	@Test
	void shouldMatchIfExporterPropertyIsTrue() {
		OnEnabledTracingExportCondition condition = new OnEnabledTracingExportCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(
				mockConditionContext(Map.of("management.tracing.export.zipkin.enabled", "true")),
				mockMetadata("zipkin"));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnEnabledTracingExport management.tracing.export.zipkin.enabled is true");
	}

	@Test
	void exporterPropertyShouldOverrideGlobalPropertyIfTrue() {
		OnEnabledTracingExportCondition condition = new OnEnabledTracingExportCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(mockConditionContext(Map
			.of("management.tracing.export.enabled", "false", "management.tracing.export.zipkin.enabled", "true")),
				mockMetadata("zipkin"));
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnEnabledTracingExport management.tracing.export.zipkin.enabled is true");
	}

	@Test
	void exporterPropertyShouldOverrideGlobalPropertyIfFalse() {
		OnEnabledTracingExportCondition condition = new OnEnabledTracingExportCondition();
		ConditionOutcome outcome = condition.getMatchOutcome(mockConditionContext(Map
			.of("management.tracing.export.enabled", "true", "management.tracing.export.zipkin.enabled", "false")),
				mockMetadata("zipkin"));
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage())
			.isEqualTo("@ConditionalOnEnabledTracingExport management.tracing.export.zipkin.enabled is false");
	}

	private ConditionContext mockConditionContext() {
		return mockConditionContext(Collections.emptyMap());
	}

	private ConditionContext mockConditionContext(Map<String, String> properties) {
		ConditionContext context = mock(ConditionContext.class);
		MockEnvironment environment = new MockEnvironment();
		properties.forEach(environment::setProperty);
		given(context.getEnvironment()).willReturn(environment);
		return context;
	}

	private AnnotatedTypeMetadata mockMetadata(String exporter) {
		AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
		given(metadata.getAnnotationAttributes(ConditionalOnEnabledTracingExport.class.getName()))
			.willReturn(Map.of("value", exporter));
		return metadata;
	}

}
