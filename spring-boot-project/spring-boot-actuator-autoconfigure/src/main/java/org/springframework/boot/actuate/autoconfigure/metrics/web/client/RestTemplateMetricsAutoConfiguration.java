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
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestTemplate}-related
 * metrics.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class, RestTemplateAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnBean(MeterRegistry.class)
public class RestTemplateMetricsAutoConfiguration {

	private final MetricsProperties properties;

	public RestTemplateMetricsAutoConfiguration(MetricsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(RestTemplateExchangeTagsProvider.class)
	public DefaultRestTemplateExchangeTagsProvider restTemplateTagConfigurer() {
		return new DefaultRestTemplateExchangeTagsProvider();
	}

	@Bean
	public MetricsRestTemplateCustomizer metricsRestTemplateCustomizer(MeterRegistry meterRegistry,
			RestTemplateExchangeTagsProvider restTemplateTagConfigurer) {
		return new MetricsRestTemplateCustomizer(meterRegistry, restTemplateTagConfigurer,
				this.properties.getWeb().getClient().getRequestsMetricName());
	}

	@Bean
	@Order(0)
	public MeterFilter metricsWebClientUriTagFilter() {
		String metricName = this.properties.getWeb().getClient().getRequestsMetricName();
		MeterFilter denyFilter = new OnlyOnceLoggingDenyMeterFilter(
				() -> String.format("Reached the maximum number of URI tags for '%s'. Are you using "
						+ "'uriVariables' on RestTemplate calls?", metricName));
		return MeterFilter.maximumAllowableTags(metricName, "uri", this.properties.getWeb().getClient().getMaxUriTags(),
				denyFilter);
	}

}
