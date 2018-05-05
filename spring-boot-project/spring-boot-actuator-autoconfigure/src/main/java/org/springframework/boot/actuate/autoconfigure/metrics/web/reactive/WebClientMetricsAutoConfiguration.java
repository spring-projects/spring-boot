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
import org.springframework.boot.actuate.autoconfigure.metrics.web.client.RestTemplateMetricsAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.reactive.client.DefaultWebClientExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of
 * {@link org.springframework.web.reactive.function.client.WebClient}.
 * <p>
 * This is reusing the {@link io.micrometer.core.instrument.config.MeterFilter} defined in
 * {@link RestTemplateMetricsAutoConfiguration} for limiting the cardinality of "uri"
 * tags.
 *
 * @author Brian Clozel
 * @since 2.1.0
 */
@Configuration
@ConditionalOnClass(WebClient.class)
@AutoConfigureAfter({ MetricsAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureBefore(WebClientAutoConfiguration.class)
@ConditionalOnBean(MeterRegistry.class)
public class WebClientMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WebClientExchangeTagsProvider defaultWebClientExchangeTagsProvider() {
		return new DefaultWebClientExchangeTagsProvider();
	}

	@Bean
	public MetricsWebClientCustomizer metricsWebClientCustomizer(
			MeterRegistry meterRegistry, WebClientExchangeTagsProvider tagsProvider,
			MetricsProperties properties) {
		return new MetricsWebClientCustomizer(meterRegistry, tagsProvider,
				properties.getWeb().getClient().getRequestsMetricName());
	}

}
