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

package org.springframework.boot.micrometer.tracing.test.autoconfigure;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfigureTracing} and {@link TracingContextCustomizerFactory}
 * working together.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class TracingContextCustomizerFactoryTests {

	private final TracingContextCustomizerFactory factory = new TracingContextCustomizerFactory();

	@Test
	void whenNotAnnotatedTracingExportIsDisabled() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatTracingExportIsDisabled(context);
	}

	@Test
	void whenAnnotatedWithDefaultAttributeTracingExportIsEnabled() {
		ContextCustomizer customizer = createContextCustomizer(TracingExportDefault.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatTracingExportIsEnabled(context);
	}

	@Test
	void whenAnnotatedWithFalseExportAttributeTracingExportIsDisabled() {
		ContextCustomizer customizer = createContextCustomizer(TracingExportDisabled.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatTracingExportIsDisabled(context);
	}

	@Test
	void whenAnnotatedWithTrueExportAttributeTracingExportIsEnabled() {
		ContextCustomizer customizer = createContextCustomizer(TracingExportEnabled.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatTracingExportIsEnabled(context);
	}

	@Test
	void notEquals() {
		ContextCustomizer customizer1 = createContextCustomizer(TracingExportEnabled.class);
		ContextCustomizer customizer2 = createContextCustomizer(TracingExportDisabled.class);
		assertThat(customizer1).isNotEqualTo(customizer2);
	}

	@Test
	void equals() {
		ContextCustomizer customizer1 = createContextCustomizer(TracingExportEnabled.class);
		ContextCustomizer customizer2 = createContextCustomizer(TracingExportEnabled.class);
		assertThat(customizer1).isEqualTo(customizer2);
		assertThat(customizer1).hasSameHashCodeAs(customizer2);
	}

	@Test
	void tracingExportCanBeEnabledViaProperty() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.tracing.export", "true");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatTracingExportIsEnabled(context);
	}

	@Test
	void tracingExportCanBeDisabledViaProperty() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.tracing.export", "false");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatTracingExportIsDisabled(context);
	}

	@Test
	void annotationTakesPrecedenceOverDisabledProperty() {
		ContextCustomizer customizer = createContextCustomizer(TracingExportEnabled.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.tracing.export", "false");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatTracingExportIsEnabled(context);
	}

	@Test
	void annotationTakesPrecedenceOverEnabledProperty() {
		ContextCustomizer customizer = createContextCustomizer(TracingExportDisabled.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.tracing.export", "true");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatTracingExportIsDisabled(context);
	}

	private void applyCustomizerToContext(ContextCustomizer customizer, ConfigurableApplicationContext context) {
		customizer.customizeContext(context,
				new MergedContextConfiguration(getClass(), null, null, null, mock(ContextLoader.class)));
	}

	private ContextCustomizer createContextCustomizer(Class<?> testClass) {
		ContextCustomizer contextCustomizer = this.factory.createContextCustomizer(testClass, Collections.emptyList());
		assertThat(contextCustomizer).as("contextCustomizer").isNotNull();
		return contextCustomizer;
	}

	private void assertThatTracingExportIsDisabled(ConfigurableApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("management.tracing.export.enabled")).isEqualTo("false");
	}

	private void assertThatTracingExportIsEnabled(ConfigurableApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("management.tracing.export.enabled")).isNull();
	}

	static class NoAnnotation {

	}

	@AutoConfigureTracing
	static class TracingExportDefault {

	}

	@AutoConfigureTracing(export = false)
	static class TracingExportDisabled {

	}

	@AutoConfigureTracing(export = true)
	static class TracingExportEnabled {

	}

}
