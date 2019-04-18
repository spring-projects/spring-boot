/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.client;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link ClientHttpRequestInterceptor} applied via a
 * {@link MetricsRestTemplateCustomizer} to record metrics.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
class MetricsClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private static final ThreadLocal<String> urlTemplate = new NamedThreadLocal<>(
			"Rest Template URL Template");

	private final MeterRegistry meterRegistry;

	private final RestTemplateExchangeTagsProvider tagProvider;

	private final String metricName;

	private final boolean autoTimeRequests;

	private final double[] percentiles;

	private final boolean histogram;

	/**
	 * Create a new {@code MetricsClientHttpRequestInterceptor}.
	 * @param meterRegistry the registry to which metrics are recorded
	 * @param tagProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @deprecated since 2.2.0 in favor of
	 * {@link #MetricsClientHttpRequestInterceptor(MeterRegistry, RestTemplateExchangeTagsProvider, String, boolean, List, boolean)}
	 */
	MetricsClientHttpRequestInterceptor(MeterRegistry meterRegistry,
			RestTemplateExchangeTagsProvider tagProvider, String metricName) {
		this(meterRegistry, tagProvider, metricName, true, null, false);
	}

	/**
	 * Create a new {@code MetricsClientHttpRequestInterceptor}.
	 * @param meterRegistry the registry to which metrics are recorded
	 * @param tagProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @param autoTimeRequests if requests should be automatically timed
	 * @param percentileList percentiles for auto time requests
	 * @param histogram histogram or not for auto time requests
	 * @since 2.2.0
	 */
	MetricsClientHttpRequestInterceptor(MeterRegistry meterRegistry,
			RestTemplateExchangeTagsProvider tagProvider, String metricName,
			boolean autoTimeRequests, List<Double> percentileList, boolean histogram) {

		double[] percentiles = (percentileList != null)
				? percentileList.stream().mapToDouble(Double::doubleValue).toArray()
				: null;

		this.tagProvider = tagProvider;
		this.meterRegistry = meterRegistry;
		this.metricName = metricName;
		this.autoTimeRequests = autoTimeRequests;
		this.percentiles = percentiles;
		this.histogram = histogram;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		long startTime = System.nanoTime();
		ClientHttpResponse response = null;
		try {
			response = execution.execute(request, body);
			return response;
		}
		finally {
			if (this.autoTimeRequests) {
				getTimeBuilder(request, response).register(this.meterRegistry)
						.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			}
			urlTemplate.remove();
		}
	}

	UriTemplateHandler createUriTemplateHandler(UriTemplateHandler delegate) {
		return new UriTemplateHandler() {

			@Override
			public URI expand(String url, Map<String, ?> arguments) {
				urlTemplate.set(url);
				return delegate.expand(url, arguments);
			}

			@Override
			public URI expand(String url, Object... arguments) {
				urlTemplate.set(url);
				return delegate.expand(url, arguments);
			}

		};
	}

	private Timer.Builder getTimeBuilder(HttpRequest request,
			ClientHttpResponse response) {
		return Timer.builder(this.metricName).publishPercentiles(this.percentiles)
				.publishPercentileHistogram(this.histogram)
				.tags(this.tagProvider.getTags(urlTemplate.get(), request, response))
				.description("Timer of RestTemplate operation");
	}

}
