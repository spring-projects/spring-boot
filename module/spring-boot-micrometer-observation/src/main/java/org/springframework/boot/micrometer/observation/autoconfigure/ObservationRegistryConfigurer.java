/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

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
 * {@link ObservationRegistry}. Also uses a {@link ObservationHandlerGroups} to group
 * handlers, which are then added to the {@link ObservationRegistry}.
 *
 * @author Moritz Halbritter
 */
class ObservationRegistryConfigurer {

	private final ObjectProvider<ObservationRegistryCustomizer<?>> customizers;

	private final ObjectProvider<ObservationPredicate> observationPredicates;

	private final ObjectProvider<GlobalObservationConvention<?>> observationConventions;

	private final ObjectProvider<ObservationHandler<?>> observationHandlers;

	private final ObjectProvider<ObservationHandlerGroup> observationHandlerGroups;

	private final ObjectProvider<ObservationFilter> observationFilters;

	ObservationRegistryConfigurer(ObjectProvider<ObservationRegistryCustomizer<?>> customizers,
			ObjectProvider<ObservationPredicate> observationPredicates,
			ObjectProvider<GlobalObservationConvention<?>> observationConventions,
			ObjectProvider<ObservationHandler<?>> observationHandlers,
			ObjectProvider<ObservationHandlerGroup> observationHandlerGroups,
			ObjectProvider<ObservationFilter> observationFilters) {
		this.customizers = customizers;
		this.observationPredicates = observationPredicates;
		this.observationConventions = observationConventions;
		this.observationHandlers = observationHandlers;
		this.observationHandlerGroups = observationHandlerGroups;
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
		ObservationHandlerGroups groups = new ObservationHandlerGroups(this.observationHandlerGroups.stream().toList());
		List<ObservationHandler<?>> orderedHandlers = this.observationHandlers.orderedStream().toList();
		groups.register(registry.observationConfig(), orderedHandlers);
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
		LambdaSafe.callbacks(ObservationRegistryCustomizer.class, this.customizers.orderedStream().toList(), registry)
			.withLogger(ObservationRegistryConfigurer.class)
			.invoke((customizer) -> customizer.customize(registry));
	}

}
