/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.metrics.autoconfigure.MetricsProperties;
import org.springframework.boot.metrics.autoconfigure.MetricsProperties.Web.Client;
import org.springframework.boot.observation.autoconfigure.ObservationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP client-related metrics.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Brian Clozel
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ ObservationProperties.class, MeterRegistry.class, MetricsProperties.class })
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public class HttpClientMetricsAutoConfiguration {

	@Bean
	@Order(0)
	MeterFilter metricsHttpClientUriTagFilter(ObservationProperties observationProperties,
			MetricsProperties metricsProperties) {
		Client clientProperties = metricsProperties.getWeb().getClient();
		String name = observationProperties.getHttp().getClient().getRequests().getName();
		MeterFilter denyFilter = new OnlyOnceLoggingDenyMeterFilter(
				() -> "Reached the maximum number of URI tags for '%s'. Are you using 'uriVariables'?".formatted(name));
		return MeterFilter.maximumAllowableTags(name, "uri", clientProperties.getMaxUriTags(), denyFilter);
	}

}
