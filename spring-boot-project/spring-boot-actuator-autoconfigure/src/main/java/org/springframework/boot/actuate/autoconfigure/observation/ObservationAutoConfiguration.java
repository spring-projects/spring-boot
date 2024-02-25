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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.aspectj.weaver.Advice;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
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
 * @since 3.0.0
 */
@AutoConfiguration(after = { CompositeMeterRegistryAutoConfiguration.class, MicrometerTracingAutoConfiguration.class })
@ConditionalOnClass(ObservationRegistry.class)
@EnableConfigurationProperties(ObservationProperties.class)
public class ObservationAutoConfiguration {

	/**
	 * Creates an instance of ObservationRegistryPostProcessor.
	 * @param observationRegistryCustomizers ObjectProvider of
	 * ObservationRegistryCustomizer instances
	 * @param observationPredicates ObjectProvider of ObservationPredicate instances
	 * @param observationConventions ObjectProvider of GlobalObservationConvention
	 * instances
	 * @param observationHandlers ObjectProvider of ObservationHandler instances
	 * @param observationHandlerGrouping ObjectProvider of ObservationHandlerGrouping
	 * instance
	 * @param observationFilters ObjectProvider of ObservationFilter instances
	 * @return an instance of ObservationRegistryPostProcessor
	 */
	@Bean
	static ObservationRegistryPostProcessor observationRegistryPostProcessor(
			ObjectProvider<ObservationRegistryCustomizer<?>> observationRegistryCustomizers,
			ObjectProvider<ObservationPredicate> observationPredicates,
			ObjectProvider<GlobalObservationConvention<?>> observationConventions,
			ObjectProvider<ObservationHandler<?>> observationHandlers,
			ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping,
			ObjectProvider<ObservationFilter> observationFilters) {
		return new ObservationRegistryPostProcessor(observationRegistryCustomizers, observationPredicates,
				observationConventions, observationHandlers, observationHandlerGrouping, observationFilters);
	}

	/**
	 * Creates an instance of ObservationRegistry if no other bean of type
	 * ObservationRegistry is present in the application context.
	 * @return the created instance of ObservationRegistry
	 */
	@Bean
	@ConditionalOnMissingBean
	ObservationRegistry observationRegistry() {
		return ObservationRegistry.create();
	}

	/**
	 * Creates a {@link PropertiesObservationFilterPredicate} bean with the specified
	 * {@link ObservationProperties}. This bean is used to filter observations based on
	 * the provided properties.
	 * @param properties the {@link ObservationProperties} used for filtering observations
	 * @return the {@link PropertiesObservationFilterPredicate} bean
	 */
	@Bean
	@Order(0)
	PropertiesObservationFilterPredicate propertiesObservationFilter(ObservationProperties properties) {
		return new PropertiesObservationFilterPredicate(properties);
	}

	/**
	 * OnlyMetricsConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnMissingClass("io.micrometer.tracing.Tracer")
	static class OnlyMetricsConfiguration {

		/**
		 * Creates a new instance of ObservationHandlerGrouping with the specified
		 * MeterObservationHandler class.
		 * @return the created ObservationHandlerGrouping instance
		 */
		@Bean
		ObservationHandlerGrouping metricsObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(MeterObservationHandler.class);
		}

	}

	/**
	 * OnlyTracingConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Tracer.class)
	@ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
	static class OnlyTracingConfiguration {

		/**
		 * Creates an instance of ObservationHandlerGrouping with
		 * TracingObservationHandler class.
		 * @return the created ObservationHandlerGrouping object
		 */
		@Bean
		ObservationHandlerGrouping tracingObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(TracingObservationHandler.class);
		}

	}

	/**
	 * MetricsWithTracingConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ MeterRegistry.class, Tracer.class })
	static class MetricsWithTracingConfiguration {

		/**
		 * Creates an instance of ObservationHandlerGrouping that includes both
		 * TracingObservationHandler and MeterObservationHandler.
		 * @return the created ObservationHandlerGrouping instance
		 */
		@Bean
		ObservationHandlerGrouping metricsAndTracingObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(
					List.of(TracingObservationHandler.class, MeterObservationHandler.class));
		}

	}

	/**
	 * MeterObservationHandlerConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(MeterRegistry.class)
	@ConditionalOnMissingBean(MeterObservationHandler.class)
	static class MeterObservationHandlerConfiguration {

		/**
		 * OnlyMetricsMeterObservationHandlerConfiguration class.
		 */
		@ConditionalOnMissingBean(type = "io.micrometer.tracing.Tracer")
		@Configuration(proxyBeanMethods = false)
		static class OnlyMetricsMeterObservationHandlerConfiguration {

			/**
			 * Creates a new instance of DefaultMeterObservationHandler with the provided
			 * MeterRegistry.
			 * @param meterRegistry the MeterRegistry to be used by the
			 * DefaultMeterObservationHandler
			 * @return a new instance of DefaultMeterObservationHandler
			 */
			@Bean
			DefaultMeterObservationHandler defaultMeterObservationHandler(MeterRegistry meterRegistry) {
				return new DefaultMeterObservationHandler(meterRegistry);
			}

		}

		/**
		 * TracingAndMetricsObservationHandlerConfiguration class.
		 */
		@ConditionalOnBean(Tracer.class)
		@Configuration(proxyBeanMethods = false)
		static class TracingAndMetricsObservationHandlerConfiguration {

			/**
			 * Creates a TracingAwareMeterObservationHandler with the given MeterRegistry
			 * and Tracer.
			 * @param meterRegistry the MeterRegistry to be used by the handler
			 * @param tracer the Tracer to be used by the handler
			 * @return a TracingAwareMeterObservationHandler with the given MeterRegistry
			 * and Tracer
			 */
			@Bean
			TracingAwareMeterObservationHandler<Observation.Context> tracingAwareMeterObservationHandler(
					MeterRegistry meterRegistry, Tracer tracer) {
				DefaultMeterObservationHandler delegate = new DefaultMeterObservationHandler(meterRegistry);
				return new TracingAwareMeterObservationHandler<>(delegate, tracer);
			}

		}

	}

	/**
	 * ObservedAspectConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Advice.class)
	static class ObservedAspectConfiguration {

		/**
		 * Creates a new instance of {@link ObservedAspect} if no other bean of the same
		 * type is present in the application context.
		 * @param observationRegistry the {@link ObservationRegistry} used by the
		 * {@link ObservedAspect}
		 * @return a new instance of {@link ObservedAspect}
		 */
		@Bean
		@ConditionalOnMissingBean
		ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
			return new ObservedAspect(observationRegistry);
		}

	}

}
