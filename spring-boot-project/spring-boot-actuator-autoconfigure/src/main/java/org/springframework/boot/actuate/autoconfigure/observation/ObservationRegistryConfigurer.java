/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation;

import java.util.List;

import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.util.LambdaSafe;

/**
 * Configurer to apply {@link ObservationRegistryCustomizer customizers} to
 * {@link ObservationRegistry observation registries}. Installs
 * {@link ObservationPredicate observation predicates} and
 * {@link GlobalObservationConvention global observation conventions} into the
 * {@link ObservationRegistry}. Also uses a {@link ObservationHandlerGrouping} to group
 * handlers, which are then added to the {@link ObservationRegistry}.
 *
 * @author Moritz Halbritter
 */
class ObservationRegistryConfigurer {

	private final ObjectProvider<ObservationRegistryCustomizer<?>> customizers;

	private final ObjectProvider<ObservationPredicate> observationPredicates;

	private final ObjectProvider<GlobalObservationConvention<?>> observationConventions;

	private final ObjectProvider<ObservationHandler<?>> observationHandlers;

	private final ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping;

	private final ObjectProvider<ObservationFilter> observationFilters;

	ObservationRegistryConfigurer(ObjectProvider<ObservationRegistryCustomizer<?>> customizers,
			ObjectProvider<ObservationPredicate> observationPredicates,
			ObjectProvider<GlobalObservationConvention<?>> observationConventions,
			ObjectProvider<ObservationHandler<?>> observationHandlers,
			ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping,
			ObjectProvider<ObservationFilter> observationFilters) {
		this.customizers = customizers;
		this.observationPredicates = observationPredicates;
		this.observationConventions = observationConventions;
		this.observationHandlers = observationHandlers;
		this.observationHandlerGrouping = observationHandlerGrouping;
		this.observationFilters = observationFilters;
	}

	void configure(ObservationRegistry registry) {
		registerObservationPredicates(registry);
		registerGlobalObservationConventions(registry);
		registerHandlers(registry);
		registerFilters(registry);
		customize(registry);
	}

	private void registerHandlers(ObservationRegistry registry) {
		this.observationHandlerGrouping.ifAvailable(
				(grouping) -> grouping.apply(asOrderedList(this.observationHandlers), registry.observationConfig()));
	}

	private void registerObservationPredicates(ObservationRegistry registry) {
		this.observationPredicates.orderedStream().forEach(registry.observationConfig()::observationPredicate);
	}

	private void registerGlobalObservationConventions(ObservationRegistry registry) {
		this.observationConventions.orderedStream().forEach(registry.observationConfig()::observationConvention);
	}

	private void registerFilters(ObservationRegistry registry) {
		this.observationFilters.orderedStream().forEach(registry.observationConfig()::observationFilter);
	}

	@SuppressWarnings("unchecked")
	private void customize(ObservationRegistry registry) {
		LambdaSafe.callbacks(ObservationRegistryCustomizer.class, asOrderedList(this.customizers), registry)
			.withLogger(ObservationRegistryConfigurer.class)
			.invoke((customizer) -> customizer.customize(registry));
	}

	private <T> List<T> asOrderedList(ObjectProvider<T> provider) {
		return provider.orderedStream().toList();
	}

}
