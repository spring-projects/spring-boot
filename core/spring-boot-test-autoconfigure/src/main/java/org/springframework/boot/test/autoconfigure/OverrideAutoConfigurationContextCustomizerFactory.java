/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} to support
 * {@link OverrideAutoConfiguration @OverrideAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class OverrideAutoConfigurationContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configurationAttributes) {
		if (AotDetector.useGeneratedArtifacts()) {
			return null;
		}
		OverrideAutoConfiguration overrideAutoConfiguration = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				OverrideAutoConfiguration.class);
		boolean enabled = (overrideAutoConfiguration == null) || overrideAutoConfiguration.enabled();
		return !enabled ? new DisableAutoConfigurationContextCustomizer() : null;
	}

	/**
	 * {@link ContextCustomizer} to disable full auto-configuration.
	 */
	private static final class DisableAutoConfigurationContextCustomizer implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			TestPropertyValues.of(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY + "=false").applyTo(context);
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			return (obj != null) && (obj.getClass() == getClass());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
