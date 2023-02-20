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

package org.springframework.boot.actuate.autoconfigure.observation.web.client;

import java.net.URI;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.web.reactive.client.DefaultWebClientExchangeTagsProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClientObservationConventionAdapter}.
 *
 * @author Brian Clozel
 */
@SuppressWarnings({ "deprecation", "removal" })
class ClientObservationConventionAdapterTests {

	private static final String TEST_METRIC_NAME = "test.metric.name";

	private final ClientObservationConventionAdapter convention = new ClientObservationConventionAdapter(
			TEST_METRIC_NAME, new DefaultWebClientExchangeTagsProvider());

	private final ClientRequest.Builder requestBuilder = ClientRequest
			.create(HttpMethod.GET, URI.create("/resource/test"))
			.attribute(WebClient.class.getName() + ".uriTemplate", "/resource/{name}");

	private final ClientResponse response = ClientResponse.create(HttpStatus.OK).body("foo").build();

	private ClientRequestObservationContext context;

	@BeforeEach
	void setup() {
		this.context = new ClientRequestObservationContext();
		this.context.setCarrier(this.requestBuilder);
		this.context.setResponse(this.response);
		this.context.setUriTemplate("/resource/{name}");
	}

	@Test
	void shouldUseConfiguredName() {
		assertThat(this.convention.getName()).isEqualTo(TEST_METRIC_NAME);
	}

	@Test
	void shouldOnlySupportClientObservationContext() {
		assertThat(this.convention.supportsContext(this.context)).isTrue();
		assertThat(this.convention.supportsContext(new OtherContext())).isFalse();
	}

	@Test
	void shouldPushTagsAsLowCardinalityKeyValues() {
		this.context.setRequest(this.requestBuilder.build());
		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).contains(KeyValue.of("status", "200"),
				KeyValue.of("outcome", "SUCCESS"), KeyValue.of("uri", "/resource/{name}"),
				KeyValue.of("method", "GET"));
	}

	@Test
	void doesNotFailWithEmptyRequest() {
		assertThat(this.convention.getLowCardinalityKeyValues(this.context)).contains(KeyValue.of("status", "200"),
				KeyValue.of("outcome", "SUCCESS"), KeyValue.of("uri", "/resource/{name}"),
				KeyValue.of("method", "GET"));
	}

	@Test
	void shouldNotPushAnyHighCardinalityKeyValue() {
		assertThat(this.convention.getHighCardinalityKeyValues(this.context)).isEmpty();
	}

	static class OtherContext extends Observation.Context {

	}

}
