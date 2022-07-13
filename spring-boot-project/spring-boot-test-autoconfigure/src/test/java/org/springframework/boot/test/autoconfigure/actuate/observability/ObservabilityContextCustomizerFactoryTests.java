/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.actuate.observability;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureObservability} and
 * {@link ObservabilityContextCustomizerFactory} working together.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class ObservabilityContextCustomizerFactoryTests {

	private final ObservabilityContextCustomizerFactory factory = new ObservabilityContextCustomizerFactory();

	@Test
	void shouldDisableBothWhenNotAnnotated() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(NoAnnotation.class,
				Collections.emptyList());
		assertThat(customizer).isNotNull();
		ConfigurableApplicationContext context = new GenericApplicationContext();
		customizer.customizeContext(context, null);
		assertThatMetricsAreDisabled(context);
		assertThatTracingIsDisabled(context);
	}

	@Test
	void shouldDisableOnlyTracing() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(OnlyMetrics.class, Collections.emptyList());
		assertThat(customizer).isNotNull();
		ConfigurableApplicationContext context = new GenericApplicationContext();
		customizer.customizeContext(context, null);
		assertThatMetricsAreEnabled(context);
		assertThatTracingIsDisabled(context);
	}

	@Test
	void shouldDisableOnlyMetrics() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(OnlyTracing.class, Collections.emptyList());
		assertThat(customizer).isNotNull();
		ConfigurableApplicationContext context = new GenericApplicationContext();
		customizer.customizeContext(context, null);
		assertThatMetricsAreDisabled(context);
		assertThatTracingIsEnabled(context);
	}

	@Test
	void shouldEnableBothWhenAnnotated() {
		ContextCustomizer customizer = this.factory.createContextCustomizer(WithAnnotation.class,
				Collections.emptyList());
		assertThat(customizer).isNotNull();
		ConfigurableApplicationContext context = new GenericApplicationContext();
		customizer.customizeContext(context, null);
		assertThatMetricsAreEnabled(context);
		assertThatTracingIsEnabled(context);
	}

	@Test
	void hashCodeAndEquals() {
		ContextCustomizer customizer1 = this.factory.createContextCustomizer(OnlyMetrics.class, null);
		ContextCustomizer customizer2 = this.factory.createContextCustomizer(OnlyTracing.class, null);
		assertThat(customizer1).isNotEqualTo(customizer2);
	}

	private void assertThatTracingIsDisabled(ConfigurableApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("management.tracing.enabled")).isEqualTo("false");
	}

	private void assertThatMetricsAreDisabled(ConfigurableApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("management.defaults.metrics.export.enabled"))
				.isEqualTo("false");
		assertThat(context.getEnvironment().getProperty("management.simple.metrics.export.enabled")).isEqualTo("true");
	}

	private void assertThatTracingIsEnabled(ConfigurableApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("management.tracing.enabled")).isNull();
	}

	private void assertThatMetricsAreEnabled(ConfigurableApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("management.defaults.metrics.export.enabled")).isNull();
		assertThat(context.getEnvironment().getProperty("management.simple.metrics.export.enabled")).isNull();
	}

	static class NoAnnotation {

	}

	@AutoConfigureObservability(tracing = false)
	static class OnlyMetrics {

	}

	@AutoConfigureObservability(metrics = false)
	static class OnlyTracing {

	}

	@AutoConfigureObservability
	static class WithAnnotation {

	}

}
