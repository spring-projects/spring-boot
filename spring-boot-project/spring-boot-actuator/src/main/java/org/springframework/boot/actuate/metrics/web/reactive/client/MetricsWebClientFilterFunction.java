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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 * {@link ExchangeFilterFunction} applied via a {@link MetricsWebClientCustomizer} to
 * record metrics.
 *
 * @author Brian Clozel
 * @since 2.1.0
 */
public class MetricsWebClientFilterFunction implements ExchangeFilterFunction {

	private static final String METRICS_WEBCLIENT_START_TIME = MetricsWebClientFilterFunction.class
			.getName() + ".START_TIME";

	private final MeterRegistry meterRegistry;

	private final WebClientExchangeTagsProvider tagProvider;

	private final String metricName;

	public MetricsWebClientFilterFunction(MeterRegistry meterRegistry,
			WebClientExchangeTagsProvider tagProvider, String metricName) {
		this.meterRegistry = meterRegistry;
		this.tagProvider = tagProvider;
		this.metricName = metricName;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest clientRequest,
			ExchangeFunction exchangeFunction) {
		return exchangeFunction.exchange(clientRequest).doOnEach((signal) -> {
			if (!signal.isOnComplete()) {
				Long startTime = signal.getContext().get(METRICS_WEBCLIENT_START_TIME);
				ClientResponse clientResponse = signal.get();
				Throwable throwable = signal.getThrowable();
				Iterable<Tag> tags = this.tagProvider.tags(clientRequest, clientResponse,
						throwable);
				Timer.builder(this.metricName).tags(tags)
						.description("Timer of WebClient operation")
						.register(this.meterRegistry)
						.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			}
		}).subscriberContext((context) -> context.put(METRICS_WEBCLIENT_START_TIME,
				System.nanoTime()));
	}

}
