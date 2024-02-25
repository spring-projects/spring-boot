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

	/**
	 * Creates a customizer for the test context based on the provided test class and
	 * configuration attributes.
	 * @param testClass the test class for which the customizer is being created
	 * @param configAttributes the configuration attributes for the test context
	 * @return the created customizer
	 */
	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureObservability annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureObservability.class);
		return new DisableObservabilityContextCustomizer(annotation);
	}

	/**
	 * DisableObservabilityContextCustomizer class.
	 */
	private static class DisableObservabilityContextCustomizer implements ContextCustomizer {

		private final AutoConfigureObservability annotation;

		/**
		 * Constructor for the DisableObservabilityContextCustomizer class.
		 * @param annotation the AutoConfigureObservability annotation to be disabled
		 */
		DisableObservabilityContextCustomizer(AutoConfigureObservability annotation) {
			this.annotation = annotation;
		}

		/**
		 * Customizes the application context based on the configuration provided. If
		 * metrics are disabled in the environment, it sets the metrics export enabled
		 * property to false and the simple metrics export enabled property to true. If
		 * tracing is disabled in the environment, it sets the tracing enabled property to
		 * false.
		 * @param context the configurable application context
		 * @param mergedContextConfiguration the merged context configuration
		 */
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

		/**
		 * Checks if metrics are disabled.
		 * @param environment the environment to check for the property
		 * @return true if metrics are disabled, false otherwise
		 */
		private boolean areMetricsDisabled(Environment environment) {
			if (this.annotation != null) {
				return !this.annotation.metrics();
			}
			return !environment.getProperty(AUTO_CONFIGURE_PROPERTY, Boolean.class, false);
		}

		/**
		 * Checks if tracing is disabled.
		 * @param environment the environment to check for the property
		 * @return true if tracing is disabled, false otherwise
		 */
		private boolean isTracingDisabled(Environment environment) {
			if (this.annotation != null) {
				return !this.annotation.tracing();
			}
			return !environment.getProperty(AUTO_CONFIGURE_PROPERTY, Boolean.class, false);
		}

		/**
		 * Compares this DisableObservabilityContextCustomizer object to the specified
		 * object.
		 * @param o the object to compare to
		 * @return true if the objects are equal, false otherwise
		 */
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

		/**
		 * Returns the hash code value for this DisableObservabilityContextCustomizer
		 * object.
		 *
		 * The hash code is computed based on the annotation field of the object.
		 * @return the hash code value for this object
		 */
		@Override
		public int hashCode() {
			return Objects.hash(this.annotation);
		}

	}

}
