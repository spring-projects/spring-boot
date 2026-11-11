/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor} that delegates to a lazily created
 * {@link ObservationRegistryConfigurer} to post-process {@link ObservationRegistry}
 * beans.
 *
 * @author Moritz Halbritter
 */
class ObservationRegistryPostProcessor implements BeanPostProcessor {

	private final ObjectProvider<ObservationRegistryCustomizer<?>> observationRegistryCustomizers;

	private final ObjectProvider<ObservationPredicate> observationPredicates;

	private final ObjectProvider<GlobalObservationConvention<?>> observationConventions;

	private final ObjectProvider<ObservationHandler<?>> observationHandlers;

	private final ObjectProvider<ObservationHandlerGroup> observationHandlerGroups;

	private final ObjectProvider<ObservationFilter> observationFilters;

	private volatile @Nullable ObservationRegistryConfigurer configurer;

	ObservationRegistryPostProcessor(ObjectProvider<ObservationRegistryCustomizer<?>> observationRegistryCustomizers,
			ObjectProvider<ObservationPredicate> observationPredicates,
			ObjectProvider<GlobalObservationConvention<?>> observationConventions,
			ObjectProvider<ObservationHandler<?>> observationHandlers,
			ObjectProvider<ObservationHandlerGroup> observationHandlerGroups,
			ObjectProvider<ObservationFilter> observationFilters) {
		this.observationRegistryCustomizers = observationRegistryCustomizers;
		this.observationPredicates = observationPredicates;
		this.observationConventions = observationConventions;
		this.observationHandlers = observationHandlers;
		this.observationHandlerGroups = observationHandlerGroups;
		this.observationFilters = observationFilters;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ObservationRegistry registry) {
			getConfigurer().configure(registry);
		}
		return bean;
	}

	private ObservationRegistryConfigurer getConfigurer() {
		ObservationRegistryConfigurer configurer = this.configurer;
		if (configurer == null) {
			configurer = new ObservationRegistryConfigurer(this.observationRegistryCustomizers,
					this.observationPredicates, this.observationConventions, this.observationHandlers,
					this.observationHandlerGroups, this.observationFilters);
			this.configurer = configurer;
		}
		return configurer;
	}

}
