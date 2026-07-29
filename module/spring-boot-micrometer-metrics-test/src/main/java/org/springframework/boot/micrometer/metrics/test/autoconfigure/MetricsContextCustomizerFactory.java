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

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} that globally disables metrics export and the use of
 * Micrometer's {@link io.micrometer.core.instrument.Metrics#globalRegistry global
 * registry} in tests. The behaviour can be controlled with {@link AutoConfigureMetrics}
 * on the test class, via the {@value #AUTO_CONFIGURE_PROPERTY} property and via the
 * {@value #USE_GLOBAL_REGISTRY_PROPERTY} property.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Lordwill Kandiro
 */
class MetricsContextCustomizerFactory implements ContextCustomizerFactory {

	static final String AUTO_CONFIGURE_PROPERTY = "spring.test.metrics.export";

	static final String USE_GLOBAL_REGISTRY_PROPERTY = "management.metrics.use-global-registry";

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureMetrics annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureMetrics.class);
		return new MetricsContextCustomizer(annotation);
	}

	private static class MetricsContextCustomizer implements ContextCustomizer {

		private final @Nullable AutoConfigureMetrics annotation;

		MetricsContextCustomizer(@Nullable AutoConfigureMetrics annotation) {
			this.annotation = annotation;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedContextConfiguration) {
			Environment environment = context.getEnvironment();
			if (areMetricsDisabled(environment)) {
				TestPropertyValues
					.of("management.defaults.metrics.export.enabled=false",
							"management.simple.metrics.export.enabled=true")
					.applyTo(context);
			}
			if (isGlobalRegistryDisabled(environment)) {
				TestPropertyValues.of(USE_GLOBAL_REGISTRY_PROPERTY + "=false").applyTo(context);
			}
		}

		private boolean areMetricsDisabled(Environment environment) {
			if (this.annotation != null) {
				return !this.annotation.export();
			}
			return !environment.getProperty(AUTO_CONFIGURE_PROPERTY, Boolean.class, false);
		}

		private boolean isGlobalRegistryDisabled(Environment environment) {
			if (this.annotation != null && this.annotation.useGlobalRegistry()) {
				return false;
			}
			return !environment.containsProperty(USE_GLOBAL_REGISTRY_PROPERTY);
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			MetricsContextCustomizer that = (MetricsContextCustomizer) o;
			return Objects.equals(this.annotation, that.annotation);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.annotation);
		}

	}

}
