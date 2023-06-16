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

import java.util.List;
import java.util.Objects;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} that globally disables metrics export and tracing in
 * tests. The behaviour can be controlled with {@link AutoConfigureObservability} on the
 * test class or via the {@value #AUTO_CONFIGURE_PROPERTY} property.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class ObservabilityContextCustomizerFactory implements ContextCustomizerFactory {

	static final String AUTO_CONFIGURE_PROPERTY = "spring.test.observability.auto-configure";

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureObservability annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureObservability.class);
		return new DisableObservabilityContextCustomizer(annotation);
	}

	private static class DisableObservabilityContextCustomizer implements ContextCustomizer {

		private final AutoConfigureObservability annotation;

		DisableObservabilityContextCustomizer(AutoConfigureObservability annotation) {
			this.annotation = annotation;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedContextConfiguration) {
			if (areMetricsDisabled(context.getEnvironment())) {
				TestPropertyValues
					.of("management.defaults.metrics.export.enabled=false",
							"management.simple.metrics.export.enabled=true")
					.applyTo(context);
			}
			if (isTracingDisabled(context.getEnvironment())) {
				TestPropertyValues.of("management.tracing.enabled=false").applyTo(context);
			}
		}

		private boolean areMetricsDisabled(Environment environment) {
			if (this.annotation != null) {
				return !this.annotation.metrics();
			}
			return !environment.getProperty(AUTO_CONFIGURE_PROPERTY, Boolean.class, false);
		}

		private boolean isTracingDisabled(Environment environment) {
			if (this.annotation != null) {
				return !this.annotation.tracing();
			}
			return !environment.getProperty(AUTO_CONFIGURE_PROPERTY, Boolean.class, false);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			DisableObservabilityContextCustomizer that = (DisableObservabilityContextCustomizer) o;
			return Objects.equals(this.annotation, that.annotation);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.annotation);
		}

	}

}
