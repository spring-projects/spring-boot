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

package org.springframework.boot.actuate.metrics.web.client;

import java.io.IOException;
import java.net.URI;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.boot.actuate.metrics.AutoTimer;
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
 */
class MetricsClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private static final ThreadLocal<Deque<String>> urlTemplate = new UrlTemplateThreadLocal();

	private final MeterRegistry meterRegistry;

	private final RestTemplateExchangeTagsProvider tagProvider;

	private final String metricName;

	private final AutoTimer autoTimer;

	/**
	 * Create a new {@code MetricsClientHttpRequestInterceptor}.
	 * @param meterRegistry the registry to which metrics are recorded
	 * @param tagProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @deprecated since 2.2.0 in favor of
	 * {@link #MetricsClientHttpRequestInterceptor(MeterRegistry, RestTemplateExchangeTagsProvider, String, AutoTimer)}
	 */
	@Deprecated
	MetricsClientHttpRequestInterceptor(MeterRegistry meterRegistry, RestTemplateExchangeTagsProvider tagProvider,
			String metricName) {
		this(meterRegistry, tagProvider, metricName, AutoTimer.ENABLED);
	}

	/**
	 * Create a new {@code MetricsClientHttpRequestInterceptor}.
	 * @param meterRegistry the registry to which metrics are recorded
	 * @param tagProvider provider for metrics tags
	 * @param metricName name of the metric to record
	 * @param autoTimer the auto-timers to apply or {@code null} to disable auto-timing
	 * @since 2.2.0
	 */
	MetricsClientHttpRequestInterceptor(MeterRegistry meterRegistry, RestTemplateExchangeTagsProvider tagProvider,
			String metricName, AutoTimer autoTimer) {
		this.tagProvider = tagProvider;
		this.meterRegistry = meterRegistry;
		this.metricName = metricName;
		this.autoTimer = (autoTimer != null) ? autoTimer : AutoTimer.DISABLED;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		if (!this.autoTimer.isEnabled()) {
			return execution.execute(request, body);
		}
		long startTime = System.nanoTime();
		ClientHttpResponse response = null;
		try {
			response = execution.execute(request, body);
			return response;
		}
		finally {
			getTimeBuilder(request, response).register(this.meterRegistry).record(System.nanoTime() - startTime,
					TimeUnit.NANOSECONDS);
			if (urlTemplate.get().isEmpty()) {
				urlTemplate.remove();
			}
		}
	}

	UriTemplateHandler createUriTemplateHandler(UriTemplateHandler delegate) {
		return new UriTemplateHandler() {

			@Override
			public URI expand(String url, Map<String, ?> arguments) {
				urlTemplate.get().push(url);
				return delegate.expand(url, arguments);
			}

			@Override
			public URI expand(String url, Object... arguments) {
				urlTemplate.get().push(url);
				return delegate.expand(url, arguments);
			}

		};
	}

	private Timer.Builder getTimeBuilder(HttpRequest request, ClientHttpResponse response) {
		return this.autoTimer.builder(this.metricName)
				.tags(this.tagProvider.getTags(urlTemplate.get().poll(), request, response))
				.description("Timer of RestTemplate operation");
	}

	private static final class UrlTemplateThreadLocal extends NamedThreadLocal<Deque<String>> {

		private UrlTemplateThreadLocal() {
			super("Rest Template URL Template");
		}

		@Override
		protected Deque<String> initialValue() {
			return new LinkedList<>();
		}

	}

}
