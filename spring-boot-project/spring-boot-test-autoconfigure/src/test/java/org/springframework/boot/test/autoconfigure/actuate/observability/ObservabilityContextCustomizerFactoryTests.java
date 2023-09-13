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

package org.springframework.boot.test.autoconfigure.actuate.observability;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;
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
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreDisabled(context);
		assertThatTracingIsDisabled(context);
	}

	@Test
	void shouldDisableOnlyTracing() {
		ContextCustomizer customizer = createContextCustomizer(OnlyMetrics.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreEnabled(context);
		assertThatTracingIsDisabled(context);
	}

	@Test
	void shouldDisableOnlyMetrics() {
		ContextCustomizer customizer = createContextCustomizer(OnlyTracing.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreDisabled(context);
		assertThatTracingIsEnabled(context);
	}

	@Test
	void shouldEnableBothWhenAnnotated() {
		ContextCustomizer customizer = createContextCustomizer(WithAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreEnabled(context);
		assertThatTracingIsEnabled(context);
	}

	@Test
	void notEquals() {
		ContextCustomizer customizer1 = createContextCustomizer(OnlyMetrics.class);
		ContextCustomizer customizer2 = createContextCustomizer(OnlyTracing.class);
		assertThat(customizer1).isNotEqualTo(customizer2);
	}

	@Test
	void equals() {
		ContextCustomizer customizer1 = createContextCustomizer(OnlyMetrics.class);
		ContextCustomizer customizer2 = createContextCustomizer(OnlyMetrics.class);
		assertThat(customizer1).isEqualTo(customizer2);
		assertThat(customizer1).hasSameHashCodeAs(customizer2);
	}

	@Test
	void metricsAndTracingCanBeEnabledViaProperty() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.observability.auto-configure", "true");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreEnabled(context);
		assertThatTracingIsEnabled(context);
	}

	@Test
	void metricsAndTracingCanBeDisabledViaProperty() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.observability.auto-configure", "false");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreDisabled(context);
		assertThatTracingIsDisabled(context);
	}

	@Test
	void annotationTakesPrecedenceOverDisabledProperty() {
		ContextCustomizer customizer = createContextCustomizer(WithAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.observability.auto-configure", "false");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreEnabled(context);
		assertThatTracingIsEnabled(context);
	}

	@Test
	void annotationTakesPrecedenceOverEnabledProperty() {
		ContextCustomizer customizer = createContextCustomizer(WithDisabledAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.observability.auto-configure", "true");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreDisabled(context);
		assertThatTracingIsDisabled(context);
	}

	private void applyCustomizerToContext(ContextCustomizer customizer, ConfigurableApplicationContext context) {
		customizer.customizeContext(context, null);
	}

	private ContextCustomizer createContextCustomizer(Class<?> testClass) {
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, Collections.emptyList());
		assertThat(contextCustomizer).as("contextCustomizer").isNotNull();
		return contextCustomizer;
	}

	private ApplicationContextInitializer<ConfigurableApplicationContext> applyCustomizer(
			ContextCustomizer customizer) {
		return (applicationContext) -> customizer.customizeContext(applicationContext, null);
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

	@AutoConfigureObservability(metrics = false, tracing = false)
	static class WithDisabledAnnotation {

	}

}
