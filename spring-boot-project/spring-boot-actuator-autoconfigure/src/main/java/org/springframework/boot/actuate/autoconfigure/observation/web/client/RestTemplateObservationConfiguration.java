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

package org.springframework.boot.actuate.autoconfigure.observation.web.client;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.web.client.RestTemplate;

/**
 * Configure the instrumentation of {@link RestTemplate}.
 *
 * @author Brian Clozel
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnBean(RestTemplateBuilder.class)
class RestTemplateObservationConfiguration {

	@Bean
	ObservationRestTemplateCustomizer observationRestTemplateCustomizer(ObservationRegistry observationRegistry,
			ObjectProvider<ClientRequestObservationConvention> customConvention,
			ObservationProperties observationProperties, MetricsProperties metricsProperties) {
		String name = observationProperties.getHttp().getClient().getRequests().getName();
		ClientRequestObservationConvention observationConvention = customConvention
			.getIfAvailable(() -> new DefaultClientRequestObservationConvention(name));
		return new ObservationRestTemplateCustomizer(observationRegistry, observationConvention);
	}

}
