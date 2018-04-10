/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.web.reactive;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.reactive.server.DefaultWebFluxTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.server.MetricsWebFilter;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring
 * WebFlux MVC annotation-based programming model request mappings.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class WebFluxMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(WebFluxTagsProvider.class)
	public DefaultWebFluxTagsProvider webfluxTagConfigurer() {
		return new DefaultWebFluxTagsProvider();
	}

	@Bean
	public MetricsWebFilter webfluxMetrics(MeterRegistry registry,
			WebFluxTagsProvider tagConfigurer, MetricsProperties properties) {
		return new MetricsWebFilter(registry, tagConfigurer,
				properties.getWeb().getServer().getRequestsMetricName());
	}

}
