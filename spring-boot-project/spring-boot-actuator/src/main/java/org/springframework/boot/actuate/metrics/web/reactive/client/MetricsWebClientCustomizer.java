/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClientCustomizer} that configures the {@link WebClient} to record request
 * metrics.
 *
 * @author Brian Clozel
 * @since 2.1.0
 */
public class MetricsWebClientCustomizer implements WebClientCustomizer {

	private final MetricsWebClientFilterFunction filterFunction;

	/**
	 * Create a new {@code MetricsWebClientFilterFunction} that will record metrics using
	 * the given {@code meterRegistry} with tags provided by the given
	 * {@code tagProvider}.
	 * @param meterRegistry the meter registry
	 * @param tagProvider the tag provider
	 * @param metricName the name of the recorded metric
	 * @param autoTimer the auto-timers to apply or {@code null} to disable auto-timing
	 * @since 2.2.0
	 */
	public MetricsWebClientCustomizer(MeterRegistry meterRegistry, WebClientExchangeTagsProvider tagProvider,
			String metricName, AutoTimer autoTimer) {
		this.filterFunction = new MetricsWebClientFilterFunction(meterRegistry, tagProvider, metricName, autoTimer);
	}

	@Override
	public void customize(WebClient.Builder webClientBuilder) {
		webClientBuilder.filters((filterFunctions) -> {
			if (!filterFunctions.contains(this.filterFunction)) {
				filterFunctions.add(0, this.filterFunction);
			}
		});
	}

}
