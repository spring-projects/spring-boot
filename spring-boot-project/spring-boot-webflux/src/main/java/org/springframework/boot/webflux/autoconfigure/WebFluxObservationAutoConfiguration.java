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

package org.springframework.boot.webflux.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.metrics.autoconfigure.MetricsProperties;
import org.springframework.boot.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring
 * WebFlux applications.
 *
 * @author Brian Clozel
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(afterName = {
		"org.springframework.boot.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration",
		"org.springframework.boot.observation.autoconfigure.ObservationAutoConfiguration" })
@ConditionalOnClass({ Observation.class, MeterRegistry.class })
@ConditionalOnBean({ ObservationRegistry.class, MeterRegistry.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public class WebFluxObservationAutoConfiguration {

	private final ObservationProperties observationProperties;

	WebFluxObservationAutoConfiguration(ObservationProperties observationProperties) {
		this.observationProperties = observationProperties;
	}

	@Bean
	@Order(0)
	MeterFilter metricsHttpServerUriTagFilter(MetricsProperties metricsProperties) {
		String name = this.observationProperties.getHttp().getServer().getRequests().getName();
		MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
				() -> "Reached the maximum number of URI tags for '%s'.".formatted(name));
		return MeterFilter.maximumAllowableTags(name, "uri", metricsProperties.getWeb().getServer().getMaxUriTags(),
				filter);
	}

	@Bean
	@ConditionalOnMissingBean(ServerRequestObservationConvention.class)
	DefaultServerRequestObservationConvention defaultServerRequestObservationConvention() {
		return new DefaultServerRequestObservationConvention(
				this.observationProperties.getHttp().getServer().getRequests().getName());
	}

}
