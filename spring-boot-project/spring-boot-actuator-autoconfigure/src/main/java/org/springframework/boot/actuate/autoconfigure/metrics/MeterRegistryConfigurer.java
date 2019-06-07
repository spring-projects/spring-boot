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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Collection;
import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.boot.util.LambdaSafe;

/**
 * Configurer to apply {@link MeterRegistryCustomizer customizers}, {@link MeterFilter
 * filters}, {@link MeterBinder binders} and {@link Metrics#addRegistry global
 * registration} to {@link MeterRegistry meter registries}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class MeterRegistryConfigurer {

	private final Collection<MeterRegistryCustomizer<?>> customizers;

	private final Collection<MeterFilter> filters;

	private final Collection<MeterBinder> binders;

	private final boolean addToGlobalRegistry;

	MeterRegistryConfigurer(Collection<MeterBinder> binders, Collection<MeterFilter> filters,
			Collection<MeterRegistryCustomizer<?>> customizers, boolean addToGlobalRegistry) {
		this.binders = (binders != null) ? binders : Collections.emptyList();
		this.filters = (filters != null) ? filters : Collections.emptyList();
		this.customizers = (customizers != null) ? customizers : Collections.emptyList();
		this.addToGlobalRegistry = addToGlobalRegistry;
	}

	void configure(MeterRegistry registry) {
		// Customizers must be applied before binders, as they may add custom
		// tags or alter timer or summary configuration.
		customize(registry);
		addFilters(registry);
		addBinders(registry);
		if (this.addToGlobalRegistry && registry != Metrics.globalRegistry) {
			Metrics.addRegistry(registry);
		}
	}

	@SuppressWarnings("unchecked")
	private void customize(MeterRegistry registry) {
		LambdaSafe.callbacks(MeterRegistryCustomizer.class, this.customizers, registry)
				.withLogger(MeterRegistryConfigurer.class).invoke((customizer) -> customizer.customize(registry));
	}

	private void addFilters(MeterRegistry registry) {
		this.filters.forEach(registry.config()::meterFilter);
	}

	private void addBinders(MeterRegistry registry) {
		this.binders.forEach((binder) -> binder.bindTo(registry));
	}

}
