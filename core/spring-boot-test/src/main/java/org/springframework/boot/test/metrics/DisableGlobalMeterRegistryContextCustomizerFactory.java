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

package org.springframework.boot.test.metrics;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} to disable the use of Micrometer's
 * {@link io.micrometer.core.instrument.Metrics#globalRegistry global registry} in tests,
 * preventing {@link io.micrometer.core.instrument.MeterRegistry meter registries} from
 * pinning application contexts when many test contexts are cached.
 */
class DisableGlobalMeterRegistryContextCustomizerFactory implements ContextCustomizerFactory {

	private static final String METRICS_CLASS = "io.micrometer.core.instrument.Metrics";

	private static final String USE_GLOBAL_REGISTRY_PROPERTY = "management.metrics.use-global-registry";

	@Override
	public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		if (ClassUtils.isPresent(METRICS_CLASS, testClass.getClassLoader())) {
			return new DisableGlobalMeterRegistryContextCustomizer();
		}
		return null;
	}

	static final class DisableGlobalMeterRegistryContextCustomizer implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			ConfigurableEnvironment environment = context.getEnvironment();
			if (environment.getProperty(USE_GLOBAL_REGISTRY_PROPERTY) == null) {
				TestPropertyValues.of(USE_GLOBAL_REGISTRY_PROPERTY + "=false").applyTo(environment);
			}
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			return obj instanceof DisableGlobalMeterRegistryContextCustomizer;
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
