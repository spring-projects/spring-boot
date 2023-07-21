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

package org.springframework.boot.actuate.autoconfigure.observation.web.reactive;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;
import org.springframework.web.filter.reactive.ServerHttpObservationFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring
 * WebFlux applications.
 *
 * @author Brian Clozel
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @since 3.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class, ObservationAutoConfiguration.class })
@ConditionalOnClass(Observation.class)
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
@SuppressWarnings("removal")
public class WebFluxObservationAutoConfiguration {

	private final ObservationProperties observationProperties;

	public WebFluxObservationAutoConfiguration(ObservationProperties observationProperties) {
		this.observationProperties = observationProperties;
	}

	@Bean
	@ConditionalOnMissingBean(ServerHttpObservationFilter.class)
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	public ServerHttpObservationFilter webfluxObservationFilter(ObservationRegistry registry,
			ObjectProvider<ServerRequestObservationConvention> customConvention) {
		String name = this.observationProperties.getHttp().getServer().getRequests().getName();
		ServerRequestObservationConvention convention = customConvention
			.getIfAvailable(() -> new DefaultServerRequestObservationConvention(name));
		return new ServerHttpObservationFilter(registry, convention);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnBean(MeterRegistry.class)
	static class MeterFilterConfiguration {

		@Bean
		@Order(0)
		MeterFilter metricsHttpServerUriTagFilter(MetricsProperties metricsProperties,
				ObservationProperties observationProperties) {
			String name = observationProperties.getHttp().getServer().getRequests().getName();
			MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
					() -> "Reached the maximum number of URI tags for '%s'.".formatted(name));
			return MeterFilter.maximumAllowableTags(name, "uri", metricsProperties.getWeb().getServer().getMaxUriTags(),
					filter);
		}

	}

}
