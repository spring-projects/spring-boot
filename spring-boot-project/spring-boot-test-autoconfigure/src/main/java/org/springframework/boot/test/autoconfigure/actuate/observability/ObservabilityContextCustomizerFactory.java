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

import java.util.List;
import java.util.Objects;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} that globally disables metrics export and tracing
 * unless {@link AutoConfigureObservability} is set on the test class.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class ObservabilityContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureObservability annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureObservability.class);
		if (annotation == null) {
			return new DisableObservabilityContextCustomizer(true, true);
		}

		return new DisableObservabilityContextCustomizer(!annotation.metrics(), !annotation.tracing());
	}

	static class DisableObservabilityContextCustomizer implements ContextCustomizer {

		private final boolean disableMetrics;

		private final boolean disableTracing;

		DisableObservabilityContextCustomizer(boolean disableMetrics, boolean disableTracing) {
			this.disableMetrics = disableMetrics;
			this.disableTracing = disableTracing;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedContextConfiguration) {
			if (this.disableMetrics) {
				TestPropertyValues.of("management.defaults.metrics.export.enabled=false",
						"management.simple.metrics.export.enabled=true").applyTo(context);
			}
			if (this.disableTracing) {
				TestPropertyValues.of("management.tracing.enabled=false").applyTo(context);
			}
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
			return this.disableMetrics == that.disableMetrics && this.disableTracing == that.disableTracing;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.disableMetrics, this.disableTracing);
		}

	}

}
