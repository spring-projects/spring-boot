/*
 * Copyright 2012-present the original author or authors.
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
 * {@link ContextCustomizerFactory} that globally disables metrics export in tests. The
 * behaviour can be controlled with {@link AutoConfigureMetrics} on the test class or via
 * the {@value #AUTO_CONFIGURE_PROPERTY} property.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MetricsContextCustomizerFactory implements ContextCustomizerFactory {

	static final String AUTO_CONFIGURE_PROPERTY = "spring.test.metrics.export";

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		AutoConfigureMetrics annotation = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				AutoConfigureMetrics.class);
		return new DisableMetricsExportContextCustomizer(annotation);
	}

	private static class DisableMetricsExportContextCustomizer implements ContextCustomizer {

		private final @Nullable AutoConfigureMetrics annotation;

		DisableMetricsExportContextCustomizer(@Nullable AutoConfigureMetrics annotation) {
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
		}

		private boolean areMetricsDisabled(Environment environment) {
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
			DisableMetricsExportContextCustomizer that = (DisableMetricsExportContextCustomizer) o;
			return Objects.equals(this.annotation, that.annotation);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.annotation);
		}

	}

}
