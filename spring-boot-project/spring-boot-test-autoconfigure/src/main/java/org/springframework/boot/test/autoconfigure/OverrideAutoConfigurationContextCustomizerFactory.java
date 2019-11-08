/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizerFactory} to support
 * {@link OverrideAutoConfiguration @OverrideAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class OverrideAutoConfigurationContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configurationAttributes) {
		boolean enabled = MergedAnnotations.from(testClass, SearchStrategy.TYPE_HIERARCHY)
				.get(OverrideAutoConfiguration.class).getValue("enabled", Boolean.class).orElse(true);
		return !enabled ? new DisableAutoConfigurationContextCustomizer() : null;
	}

	/**
	 * {@link ContextCustomizer} to disable full auto-configuration.
	 */
	private static class DisableAutoConfigurationContextCustomizer implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			TestPropertyValues.of(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY + "=false").applyTo(context);
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null) && (obj.getClass() == getClass());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
