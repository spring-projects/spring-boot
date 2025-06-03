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

package org.springframework.boot.actuate.autoconfigure.observability;

import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler.IgnoredMeters;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;

import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.observation.autoconfigure.ObservationHandlerGrouping;
import org.springframework.boot.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Micrometer Observation API.
 *
 * @author Moritz Halbritter
 * @author Brian Clozel
 * @author Jonatan Ivanov
 * @author Vedran Pavic
 * @since 3.0.0
 */
@AutoConfiguration(beforeName = "org.springframework.boot.observation.autoconfigure.ObservationAutoConfiguration",
		afterName = "org.springframework.boot.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
		after = MicrometerTracingAutoConfiguration.class)
@ConditionalOnClass({ ObservationRegistry.class, ObservationHandlerGrouping.class })
public class ObservabilityAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnMissingClass("io.micrometer.tracing.Tracer")
	static class OnlyMetricsConfiguration {

		@Bean
		ObservationHandlerGrouping metricsObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(MeterObservationHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Tracer.class)
	@ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
	static class OnlyTracingConfiguration {

		@Bean
		ObservationHandlerGrouping tracingObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(TracingObservationHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ MeterRegistry.class, Tracer.class })
	static class MetricsWithTracingConfiguration {

		@Bean
		ObservationHandlerGrouping metricsAndTracingObservationHandlerGrouping() {
			return new ObservationHandlerGrouping(
					List.of(TracingObservationHandler.class, MeterObservationHandler.class));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(MeterRegistry.class)
	@ConditionalOnMissingBean(MeterObservationHandler.class)
	@EnableConfigurationProperties(ObservationProperties.class)
	static class MeterObservationHandlerConfiguration {

		@ConditionalOnMissingBean(type = "io.micrometer.tracing.Tracer")
		@Configuration(proxyBeanMethods = false)
		static class OnlyMetricsMeterObservationHandlerConfiguration {

			@Bean
			DefaultMeterObservationHandler defaultMeterObservationHandler(MeterRegistry meterRegistry,
					ObservationProperties properties) {
				return properties.getLongTaskTimer().isEnabled() ? new DefaultMeterObservationHandler(meterRegistry)
						: new DefaultMeterObservationHandler(meterRegistry, IgnoredMeters.LONG_TASK_TIMER);
			}

		}

		@ConditionalOnBean(Tracer.class)
		@Configuration(proxyBeanMethods = false)
		static class TracingAndMetricsObservationHandlerConfiguration {

			@Bean
			TracingAwareMeterObservationHandler<Observation.Context> tracingAwareMeterObservationHandler(
					MeterRegistry meterRegistry, Tracer tracer, ObservationProperties properties) {
				DefaultMeterObservationHandler delegate = properties.getLongTaskTimer().isEnabled()
						? new DefaultMeterObservationHandler(meterRegistry)
						: new DefaultMeterObservationHandler(meterRegistry, IgnoredMeters.LONG_TASK_TIMER);
				return new TracingAwareMeterObservationHandler<>(delegate, tracer);
			}

		}

	}

}
