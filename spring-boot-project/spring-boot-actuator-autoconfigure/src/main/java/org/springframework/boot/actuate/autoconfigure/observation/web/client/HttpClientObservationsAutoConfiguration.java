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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Web.Client;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP client-related
 * observations.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Brian Clozel
 * @since 3.0.0
 */
@AutoConfiguration(after = { ObservationAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		RestTemplateAutoConfiguration.class, WebClientAutoConfiguration.class })
@ConditionalOnClass(Observation.class)
@ConditionalOnBean(ObservationRegistry.class)
@Import({ RestTemplateObservationConfiguration.class, WebClientObservationConfiguration.class })
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public class HttpClientObservationsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnBean(MeterRegistry.class)
	static class MeterFilterConfiguration {

		@Bean
		@Order(0)
		MeterFilter metricsHttpClientUriTagFilter(ObservationProperties observationProperties,
				MetricsProperties metricsProperties) {
			Client clientProperties = metricsProperties.getWeb().getClient();
			String name = observationProperties.getHttp().getClient().getRequests().getName();
			MeterFilter denyFilter = new OnlyOnceLoggingDenyMeterFilter(
					() -> "Reached the maximum number of URI tags for '%s'. Are you using 'uriVariables'?"
						.formatted(name));
			return MeterFilter.maximumAllowableTags(name, "uri", clientProperties.getMaxUriTags(), denyFilter);
		}

	}

}
