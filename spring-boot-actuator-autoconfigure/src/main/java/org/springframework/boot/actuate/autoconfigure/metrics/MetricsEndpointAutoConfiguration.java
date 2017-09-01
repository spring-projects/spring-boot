/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.PublicMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link MetricsEndpoint}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration
@AutoConfigureAfter(PublicMetricsAutoConfiguration.class)
public class MetricsEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public MetricsEndpoint metricsEndpoint(
			ObjectProvider<List<PublicMetrics>> publicMetrics) {
		return metricsEndpoint(publicMetrics.getIfAvailable(Collections::emptyList));
	}

	private MetricsEndpoint metricsEndpoint(List<PublicMetrics> publicMetrics) {
		return new MetricsEndpoint(sort(publicMetrics));
	}

	private List<PublicMetrics> sort(List<PublicMetrics> publicMetrics) {
		List<PublicMetrics> sorted = new ArrayList<>(publicMetrics);
		Collections.sort(sorted, AnnotationAwareOrderComparator.INSTANCE);
		return sorted;
	}

}
