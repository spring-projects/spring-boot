/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.web.reactive;

import io.micrometer.common.KeyValue;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.web.reactive.server.DefaultWebFluxTagsProvider;
import org.springframework.http.observation.reactive.ServerRequestObservationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerRequestObservationConventionAdapter}.
 *
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.0.0", forRemoval = true)
class ServerRequestObservationConventionAdapterTests {

	private static final String TEST_METRIC_NAME = "test.metric.name";

	private final MockServerHttpRequest request = MockServerHttpRequest.get("/resource/test").build();

	private final MockServerWebExchange serverWebExchange = MockServerWebExchange.builder(this.request).build();

	private final ServerRequestObservationContext context = new ServerRequestObservationContext(this.serverWebExchange);

	private final ServerRequestObservationConventionAdapter convention = new ServerRequestObservationConventionAdapter(
			TEST_METRIC_NAME, new DefaultWebFluxTagsProvider());

	@Test
	void shouldUseConfiguredName() {
		assertThat(this.convention.getName()).isEqualTo(TEST_METRIC_NAME);
	}

	@Test
	void shouldPushTagsAsLowCardinalityKeyValues() {
		this.serverWebExchange.getAttributes().put(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
				PathPatternParser.defaultInstance.parse("/resource/{name}"));
		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).contains(KeyValue.of("status", "200"),
				KeyValue.of("outcome", "SUCCESS"), KeyValue.of("uri", "/resource/{name}"),
				KeyValue.of("method", "GET"));
	}

}
