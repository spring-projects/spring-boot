/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.actuate.metrics;

import java.util.List;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} that globally disables metrics export unless
 * {@link AutoConfigureMetrics} is set on the test class.
 *
 * @author Chris Bono
 */
class MetricsExportContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		boolean disableMetricsExport = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureMetrics.class) == null;
		return disableMetricsExport ? new DisableMetricExportContextCustomizer() : null;
	}

	static class DisableMetricExportContextCustomizer implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedContextConfiguration) {
			TestPropertyValues.of("management.metrics.export.defaults.enabled=false",
					"management.metrics.export.simple.enabled=true").applyTo(context);
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null) && (getClass() == obj.getClass());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
