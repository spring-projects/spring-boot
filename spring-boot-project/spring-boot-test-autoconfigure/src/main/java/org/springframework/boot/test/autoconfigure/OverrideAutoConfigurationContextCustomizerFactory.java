/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.autoconfigure;

import java.util.List;

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

	/**
     * Creates a customizer for the test context based on the provided test class and configuration attributes.
     * If AOT detection is enabled, returns null.
     * If the test class has the @OverrideAutoConfiguration annotation with enabled set to false, returns a DisableAutoConfigurationContextCustomizer.
     * Otherwise, returns null.
     *
     * @param testClass                the test class for which the customizer is created
     * @param configurationAttributes the configuration attributes for the test context
     * @return the customizer for the test context, or null if AOT detection is enabled or @OverrideAutoConfiguration is disabled
     */
    @Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
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

		/**
         * Customize the application context by disabling auto-configuration.
         * This method is called during the initialization of the application context.
         * It sets the property "spring.autoconfigure.enabled.override" to "false" in order to disable auto-configuration.
         *
         * @param context the configurable application context
         * @param mergedConfig the merged context configuration
         */
        @Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			TestPropertyValues.of(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY + "=false").applyTo(context);
		}

		/**
         * Compares this object with the specified object for equality.
         * 
         * @param obj the object to compare with
         * @return true if the specified object is of the same class as this object, false otherwise
         */
        @Override
		public boolean equals(Object obj) {
			return (obj != null) && (obj.getClass() == getClass());
		}

		/**
         * Returns a hash code value for the object. This method overrides the default implementation of the hashCode() method.
         *
         * @return the hash code value for this object
         */
        @Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
