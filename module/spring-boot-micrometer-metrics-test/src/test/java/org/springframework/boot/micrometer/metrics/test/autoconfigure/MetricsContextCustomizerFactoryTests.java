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

package org.springframework.boot.micrometer.metrics.test.autoconfigure;

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
 * Tests for {@link AutoConfigureMetrics} and {@link MetricsContextCustomizerFactory}
 * working together.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MetricsContextCustomizerFactoryTests {

	private final MetricsContextCustomizerFactory factory = new MetricsContextCustomizerFactory();

	@Test
	void whenNotAnnotatedMetricsExportIsDisabled() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreDisabled(context);
	}

	@Test
	void whenAnnotatedWithDefaultAttributeMetricsExportIsEnabled() {
		ContextCustomizer customizer = createContextCustomizer(MetricsExportDefault.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreEnabled(context);
	}

	@Test
	void whenAnnotatedWithFalseExportAttributeMetricsExportIsDisabled() {
		ContextCustomizer customizer = createContextCustomizer(MetricsExportDisabled.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreDisabled(context);
	}

	@Test
	void whenAnnotatedWithTrueExportAttributeMetricsExportIsEnabled() {
		ContextCustomizer customizer = createContextCustomizer(MetricsExportEnabled.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreEnabled(context);
	}

	@Test
	void notEquals() {
		ContextCustomizer customizer1 = createContextCustomizer(MetricsExportEnabled.class);
		ContextCustomizer customizer2 = createContextCustomizer(MetricsExportDisabled.class);
		assertThat(customizer1).isNotEqualTo(customizer2);
	}

	@Test
	void equals() {
		ContextCustomizer customizer1 = createContextCustomizer(MetricsExportEnabled.class);
		ContextCustomizer customizer2 = createContextCustomizer(MetricsExportEnabled.class);
		assertThat(customizer1).isEqualTo(customizer2);
		assertThat(customizer1).hasSameHashCodeAs(customizer2);
	}

	@Test
	void metricsExportCanBeEnabledViaProperty() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.metrics.export", "true");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreEnabled(context);
	}

	@Test
	void metricsExportCanBeDisabledViaProperty() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.metrics.export", "false");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreDisabled(context);
	}

	@Test
	void annotationTakesPrecedenceOverDisabledProperty() {
		ContextCustomizer customizer = createContextCustomizer(MetricsExportEnabled.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.metrics.export", "false");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreEnabled(context);
	}

	@Test
	void annotationTakesPrecedenceOverEnabledProperty() {
		ContextCustomizer customizer = createContextCustomizer(MetricsExportDisabled.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring.test.metrics.export", "true");
		context.setEnvironment(environment);
		applyCustomizerToContext(customizer, context);
		assertThatMetricsAreDisabled(context);
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

	private void assertThatMetricsAreDisabled(ConfigurableApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("management.defaults.metrics.export.enabled"))
			.isEqualTo("false");
		assertThat(context.getEnvironment().getProperty("management.simple.metrics.export.enabled")).isEqualTo("true");
	}

	private void assertThatMetricsAreEnabled(ConfigurableApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("management.defaults.metrics.export.enabled")).isNull();
		assertThat(context.getEnvironment().getProperty("management.simple.metrics.export.enabled")).isNull();
	}

	static class NoAnnotation {

	}

	@AutoConfigureMetrics
	static class MetricsExportDefault {

	}

	@AutoConfigureMetrics(export = false)
	static class MetricsExportDisabled {

	}

	@AutoConfigureMetrics(export = true)
	static class MetricsExportEnabled {

	}

}
