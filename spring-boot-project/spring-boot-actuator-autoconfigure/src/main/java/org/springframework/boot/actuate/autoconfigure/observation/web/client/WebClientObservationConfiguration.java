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

package org.springframework.boot.actuate.autoconfigure.observation.web.client;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.actuate.metrics.web.reactive.client.ObservationWebClientCustomizer;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configure the instrumentation of {@link WebClient}.
 *
 * @author Brian Clozel
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
@SuppressWarnings("removal")
class WebClientObservationConfiguration {

	@Bean
	ObservationWebClientCustomizer observationWebClientCustomizer(ObservationRegistry observationRegistry,
			ObjectProvider<ClientRequestObservationConvention> customConvention,
			ObservationProperties observationProperties, ObjectProvider<WebClientExchangeTagsProvider> tagsProvider,
			MetricsProperties metricsProperties) {
		String name = observationName(observationProperties, metricsProperties);
		ClientRequestObservationConvention observationConvention = createConvention(customConvention.getIfAvailable(),
				tagsProvider.getIfAvailable(), name);
		return new ObservationWebClientCustomizer(observationRegistry, observationConvention);
	}

	private static ClientRequestObservationConvention createConvention(
			ClientRequestObservationConvention customConvention, WebClientExchangeTagsProvider tagsProvider,
			String name) {
		if (customConvention != null) {
			return customConvention;
		}
		else if (tagsProvider != null) {
			return new ClientObservationConventionAdapter(name, tagsProvider);
		}
		else {
			return new DefaultClientRequestObservationConvention(name);
		}
	}

	private static String observationName(ObservationProperties observationProperties,
			MetricsProperties metricsProperties) {
		String metricName = metricsProperties.getWeb().getClient().getRequest().getMetricName();
		String observationName = observationProperties.getHttp().getClient().getRequests().getName();
		return (observationName != null) ? observationName : metricName;
	}

}
