/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web.client;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Web.Client.ClientRequest;
import org.springframework.boot.actuate.metrics.web.reactive.client.DefaultWebClientExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configure the instrumentation of {@link WebClient}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
class WebClientMetricsConfiguration {

	@Bean
	@ConditionalOnMissingBean
	WebClientExchangeTagsProvider defaultWebClientExchangeTagsProvider() {
		return new DefaultWebClientExchangeTagsProvider();
	}

	@Bean
	MetricsWebClientCustomizer metricsWebClientCustomizer(MeterRegistry meterRegistry,
			WebClientExchangeTagsProvider tagsProvider, MetricsProperties properties) {
		ClientRequest request = properties.getWeb().getClient().getRequest();
		return new MetricsWebClientCustomizer(meterRegistry, tagsProvider, request.getMetricName(),
				request.getAutotime());
	}

}
