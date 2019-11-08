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
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configure the instrumentation of {@link RestTemplate}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Raheela Aslam
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnBean(RestTemplateBuilder.class)
class RestTemplateMetricsConfiguration {

	@Bean
	@ConditionalOnMissingBean(RestTemplateExchangeTagsProvider.class)
	DefaultRestTemplateExchangeTagsProvider restTemplateExchangeTagsProvider() {
		return new DefaultRestTemplateExchangeTagsProvider();
	}

	@Bean
	MetricsRestTemplateCustomizer metricsRestTemplateCustomizer(MeterRegistry meterRegistry,
			RestTemplateExchangeTagsProvider restTemplateExchangeTagsProvider, MetricsProperties properties) {
		ClientRequest request = properties.getWeb().getClient().getRequest();
		return new MetricsRestTemplateCustomizer(meterRegistry, restTemplateExchangeTagsProvider,
				request.getMetricName(), request.getAutotime());
	}

}
