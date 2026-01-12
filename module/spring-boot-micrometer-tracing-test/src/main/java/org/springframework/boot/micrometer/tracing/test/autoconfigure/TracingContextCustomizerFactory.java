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
 * {@link ContextCustomizerFactory} that globally disables metrics export in tests. The
 * behaviour can be controlled with {@link AutoConfigureTracing} on the test class or via
 * the {@value #AUTO_CONFIGURE_PROPERTY} property.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class TracingContextCustomizerFactory implements ContextCustomizerFactory {

	static final String AUTO_CONFIGURE_PROPERTY = "spring.test.tracing.export";

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureTracing annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureTracing.class);
		return new DisableTracingExportContextCustomizer(annotation);
	}

	private static class DisableTracingExportContextCustomizer implements ContextCustomizer {

		private final @Nullable AutoConfigureTracing annotation;

		DisableTracingExportContextCustomizer(@Nullable AutoConfigureTracing annotation) {
			this.annotation = annotation;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedContextConfiguration) {
			if (isTracingDisabled(context.getEnvironment())) {
				TestPropertyValues.of("management.tracing.export.enabled=false").applyTo(context);
			}
		}

		private boolean isTracingDisabled(Environment environment) {
			if (this.annotation != null) {
				return !this.annotation.export();
			}
			return !environment.getProperty(AUTO_CONFIGURE_PROPERTY, Boolean.class, false);
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			DisableTracingExportContextCustomizer that = (DisableTracingExportContextCustomizer) o;
			return Objects.equals(this.annotation, that.annotation);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.annotation);
		}

	}

}
