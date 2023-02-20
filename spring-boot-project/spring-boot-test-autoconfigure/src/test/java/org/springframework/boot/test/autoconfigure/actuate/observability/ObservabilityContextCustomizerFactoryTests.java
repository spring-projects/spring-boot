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

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
	void shouldRegisterNoopTracerIfTracingIsDisabled() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		context.refresh();
		Tracer tracer = context.getBean(Tracer.class);
		assertThat(tracer).isNotNull();
		assertThat(tracer.nextSpan().isNoop()).isTrue();
	}

	@Test
	void shouldNotRegisterNoopTracerIfTracingIsEnabled() {
		ContextCustomizer customizer = createContextCustomizer(WithAnnotation.class);
		ConfigurableApplicationContext context = new GenericApplicationContext();
		applyCustomizerToContext(customizer, context);
		context.refresh();
		assertThat(context.getBeanProvider(Tracer.class).getIfAvailable()).as("Tracer bean").isNull();
	}

	@Test
	void shouldNotRegisterNoopTracerIfMicrometerTracingIsNotPresent() throws Exception {
		try (FilteredClassLoader filteredClassLoader = new FilteredClassLoader("io.micrometer.tracing")) {
			ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
			new ApplicationContextRunner().withClassLoader(filteredClassLoader)
					.withInitializer(applyCustomizer(customizer)).run((context) -> {
						assertThat(context).doesNotHaveBean(Tracer.class);
						assertThatMetricsAreDisabled(context);
						assertThatTracingIsDisabled(context);
					});
		}
	}

	@Test
	void shouldBackOffOnCustomTracer() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		new ApplicationContextRunner().withConfiguration(UserConfigurations.of(CustomTracer.class))
				.withInitializer(applyCustomizer(customizer)).run((context) -> {
					assertThat(context).hasSingleBean(Tracer.class);
					assertThat(context).hasBean("customTracer");
				});
	}

	@Test
	void shouldNotRunIfAotIsEnabled() {
		ContextCustomizer customizer = createContextCustomizer(NoAnnotation.class);
		new ApplicationContextRunner().withSystemProperties("spring.aot.enabled:true")
				.withInitializer(applyCustomizer(customizer))
				.run((context) -> assertThat(context).doesNotHaveBean(Tracer.class));
	}

	@Test
	void hashCodeAndEquals() {
		ContextCustomizer customizer1 = createContextCustomizer(OnlyMetrics.class);
		ContextCustomizer customizer2 = createContextCustomizer(OnlyTracing.class);
		assertThat(customizer1).isNotEqualTo(customizer2);
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

	@Configuration(proxyBeanMethods = false)
	static class CustomTracer {

		@Bean
		Tracer customTracer() {
			return mock(Tracer.class);
		}

	}

}
