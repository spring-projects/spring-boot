/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservationKeyValueAnnotationHandler;
import io.micrometer.observation.aop.ObservedAspect;
import org.aspectj.weaver.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Micrometer Observation API.
 *
 * @author Moritz Halbritter
 * @author Brian Clozel
 * @author Jonatan Ivanov
 * @author Vedran Pavic
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(ObservationRegistry.class)
@EnableConfigurationProperties(ObservationProperties.class)
public final class ObservationAutoConfiguration {

	@Bean
	static ObservationRegistryPostProcessor observationRegistryPostProcessor(
			ObjectProvider<ObservationRegistryCustomizer<?>> observationRegistryCustomizers,
			ObjectProvider<ObservationPredicate> observationPredicates,
			ObjectProvider<GlobalObservationConvention<?>> observationConventions,
			ObjectProvider<ObservationHandler<?>> observationHandlers,
			ObjectProvider<ObservationHandlerGroup> observationHandlerGroups,
			ObjectProvider<ObservationFilter> observationFilters) {
		return new ObservationRegistryPostProcessor(observationRegistryCustomizers, observationPredicates,
				observationConventions, observationHandlers, observationHandlerGroups, observationFilters);
	}

	@Bean
	@ConditionalOnMissingBean
	ObservationRegistry observationRegistry() {
		return ObservationRegistry.create();
	}

	@Bean
	@Order(0)
	PropertiesObservationFilterPredicate propertiesObservationFilter(ObservationProperties properties) {
		return new PropertiesObservationFilterPredicate(properties);
	}

	@Bean
	@ConditionalOnMissingBean(ValueExpressionResolver.class)
	SpelValueExpressionResolver spelValueExpressionResolver() {
		return new SpelValueExpressionResolver();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Advice.class)
	@ConditionalOnBooleanProperty("management.observations.annotations.enabled")
	static class ObservedAspectConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ObservedAspect observedAspect(ObservationRegistry observationRegistry,
				ObjectProvider<ObservationKeyValueAnnotationHandler> observationKeyValueAnnotationHandler) {
			ObservedAspect observedAspect = new ObservedAspect(observationRegistry);
			observationKeyValueAnnotationHandler.ifAvailable(observedAspect::setObservationKeyValueAnnotationHandler);
			return observedAspect;
		}

		@Bean
		@ConditionalOnMissingBean
		ObservationKeyValueAnnotationHandler observationKeyValueAnnotationHandler(BeanFactory beanFactory,
				ValueExpressionResolver valueExpressionResolver) {
			return new ObservationKeyValueAnnotationHandler(beanFactory::getBean, (ignored) -> valueExpressionResolver);
		}

	}

}
