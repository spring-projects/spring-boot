/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Collection;
import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Applies autowired {@link MeterBinder}s and {@link MeterRegistryCustomizer}s to a configured {@link MeterRegistry}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Component
public class MeterRegistryPostProcessor implements BeanPostProcessor {
	private final MetricsProperties config;

	private final Collection<MeterBinder> binders;

	private final Collection<MeterRegistryCustomizer> customizers;

	@SuppressWarnings("ConstantConditions")
	MeterRegistryPostProcessor(MetricsProperties config,
			ObjectProvider<Collection<MeterBinder>> binders,
			ObjectProvider<Collection<MeterRegistryCustomizer>> customizers) {
		this.config = config;
		this.binders = binders.getIfAvailable() != null ? binders.getIfAvailable() : Collections.emptyList();
		this.customizers = customizers.getIfAvailable() != null ? customizers.getIfAvailable() : Collections.emptyList();
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof MeterRegistry) {
			MeterRegistry registry = (MeterRegistry) bean;

			// Customizers must be applied before binders, as they may add custom tags or alter
			// timer or summary configuration.
			this.customizers.forEach(c -> c.configureRegistry(registry));

			this.binders.forEach(b -> b.bindTo(registry));

			if (this.config.isUseGlobalRegistry() && registry != Metrics.globalRegistry) {
				Metrics.addRegistry(registry);
			}
		}

		return bean;
	}
}
