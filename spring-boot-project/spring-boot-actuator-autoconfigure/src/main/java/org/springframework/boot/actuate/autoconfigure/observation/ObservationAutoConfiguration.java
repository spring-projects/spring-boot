/*
 * Copyright 2012-2022 the original author or authors.
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation.GlobalKeyValuesProvider;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.TracingObservationHandler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Micrometer Observation API.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@AutoConfiguration(after = CompositeMeterRegistryAutoConfiguration.class)
@ConditionalOnClass(ObservationRegistry.class)
public class ObservationAutoConfiguration {

	@Bean
	static ObservationRegistryPostProcessor observationRegistryPostProcessor(
			ObjectProvider<ObservationRegistryCustomizer<?>> observationRegistryCustomizers,
			ObjectProvider<ObservationPredicate> observationPredicates,
			ObjectProvider<GlobalKeyValuesProvider<?>> keyValuesProviders,
			ObjectProvider<ObservationHandler<?>> observationHandlers,
			ObjectProvider<ObservationHandlerGrouping> observationHandlerGrouping) {
		return new ObservationRegistryPostProcessor(observationRegistryCustomizers, observationPredicates,
				keyValuesProviders, observationHandlers, observationHandlerGrouping);
	}

	@Bean
	@ConditionalOnMissingBean
	ObservationRegistry observationRegistry() {
		return ObservationRegistry.create();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(MeterRegistry.class)
	static class MetricsConfiguration {

		@Bean
		TimerObservationHandlerObservationRegistryCustomizer enableTimerObservationHandler(
				MeterRegistry meterRegistry) {
			return new TimerObservationHandlerObservationRegistryCustomizer(meterRegistry);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("io.micrometer.tracing.handler.TracingObservationHandler")
	static class OnlyMetricsConfiguration {

		@Bean
		OnlyMetricsObservationHandlerGrouping onlyMetricsObservationHandlerGrouping() {
			return new OnlyMetricsObservationHandlerGrouping();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(TracingObservationHandler.class)
	static class TracingConfiguration {

		@Bean
		TracingObservationHandlerGrouping tracingObservationHandlerGrouping() {
			return new TracingObservationHandlerGrouping();
		}

	}

}
