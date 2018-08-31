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

package org.springframework.boot.actuate.autoconfigure.metrics.web;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for register {@link RestTemplate} or {@link WebClient} metrics
 * filters.
 *
 * @author Dmytro Nosan
 * @since 2.0.5
 */
@Configuration
@AutoConfigureAfter({ MetricsAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnBean(MeterRegistry.class)
@Conditional(WebMetricsClientFiltersAutoConfiguration.AnyWebMetricsClientAvailableCondition.class)
public class WebMetricsClientFiltersAutoConfiguration {

	private final MetricsProperties properties;

	public WebMetricsClientFiltersAutoConfiguration(MetricsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@Order(0)
	public MeterFilter webMetricsClientUriTagFilter() {
		MetricsProperties.Web.Client client = this.properties.getWeb().getClient();
		String metricName = client.getRequestsMetricName();
		MeterFilter denyFilter = new OnlyOnceLoggingDenyMeterFilter(
				() -> String.format(
						"Reached the maximum number of URI tags for '%s'. Are you using "
								+ "'uriVariables' on RestTemplate/WebClient calls?",
						metricName));
		return MeterFilter.maximumAllowableTags(metricName, "uri", client.getMaxUriTags(),
				denyFilter);
	}

	/**
	 * Check if either a {@link RestTemplate} or {@link WebClient} class is available.
	 */
	static class AnyWebMetricsClientAvailableCondition extends AnyNestedCondition {

		AnyWebMetricsClientAvailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(RestTemplate.class)
		static class RestTemplateAvailable {

		}

		@ConditionalOnClass(WebClient.class)
		static class WebClientAvailable {

		}

	}

}
