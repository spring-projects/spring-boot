/*
 * Copyright 2012-2021 the original author or authors.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsWebClientCustomizer}
 *
 * @author Brian Clozel
 */
class MetricsWebClientCustomizerTests {

	private MetricsWebClientCustomizer customizer;

	private WebClient.Builder clientBuilder;

	@BeforeEach
	void setup() {
		this.customizer = new MetricsWebClientCustomizer(mock(MeterRegistry.class),
				mock(WebClientExchangeTagsProvider.class), "test", null);
		this.clientBuilder = WebClient.builder();
	}

	@Test
	void customizeShouldAddFilterFunction() {
		this.clientBuilder.filter(mock(ExchangeFilterFunction.class));
		this.customizer.customize(this.clientBuilder);
		this.clientBuilder.filters(
				(filters) -> assertThat(filters).hasSize(2).first().isInstanceOf(MetricsWebClientFilterFunction.class));
	}

	@Test
	void customizeShouldNotAddDuplicateFilterFunction() {
		this.customizer.customize(this.clientBuilder);
		this.clientBuilder.filters((filters) -> assertThat(filters).hasSize(1));
		this.customizer.customize(this.clientBuilder);
		this.clientBuilder.filters(
				(filters) -> assertThat(filters).singleElement().isInstanceOf(MetricsWebClientFilterFunction.class));
	}

}
