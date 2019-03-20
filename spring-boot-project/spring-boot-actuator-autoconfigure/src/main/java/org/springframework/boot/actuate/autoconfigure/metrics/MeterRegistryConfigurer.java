/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.List;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.beans.factory.ObjectProvider;
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

	private final ObjectProvider<MeterRegistryCustomizer<?>> customizers;

	private final ObjectProvider<MeterFilter> filters;

	private final ObjectProvider<MeterBinder> binders;

	private final boolean addToGlobalRegistry;

	MeterRegistryConfigurer(ObjectProvider<MeterRegistryCustomizer<?>> customizers,
			ObjectProvider<MeterFilter> filters, ObjectProvider<MeterBinder> binders,
			boolean addToGlobalRegistry) {
		this.customizers = customizers;
		this.filters = filters;
		this.binders = binders;
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
		LambdaSafe
				.callbacks(MeterRegistryCustomizer.class, asOrderedList(this.customizers),
						registry)
				.withLogger(MeterRegistryConfigurer.class)
				.invoke((customizer) -> customizer.customize(registry));
	}

	private void addFilters(MeterRegistry registry) {
		this.filters.orderedStream().forEach(registry.config()::meterFilter);
	}

	private void addBinders(MeterRegistry registry) {
		this.binders.orderedStream().forEach((binder) -> binder.bindTo(registry));
	}

	private <T> List<T> asOrderedList(ObjectProvider<T> provider) {
		return provider.orderedStream().collect(Collectors.toList());
	}

}
