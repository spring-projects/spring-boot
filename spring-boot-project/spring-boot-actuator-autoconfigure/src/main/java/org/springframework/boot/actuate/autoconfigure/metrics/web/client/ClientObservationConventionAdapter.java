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

package org.springframework.boot.actuate.autoconfigure.metrics.web.client;

import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.Observation;

import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.web.reactive.function.client.ClientObservationContext;
import org.springframework.web.reactive.function.client.ClientObservationConvention;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Adapter class that applies {@link WebClientExchangeTagsProvider} tags as a
 * {@link ClientObservationConvention}.
 *
 * @author Brian Clozel
 */
@SuppressWarnings({ "deprecation", "removal" })
class ClientObservationConventionAdapter implements ClientObservationConvention {

	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

	private final String metricName;

	private final WebClientExchangeTagsProvider tagsProvider;

	ClientObservationConventionAdapter(String metricName, WebClientExchangeTagsProvider tagsProvider) {
		this.metricName = metricName;
		this.tagsProvider = tagsProvider;
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof ClientObservationContext;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ClientObservationContext context) {
		KeyValues keyValues = KeyValues.empty();
		mutateClientRequest(context);
		Iterable<Tag> tags = this.tagsProvider.tags(context.getCarrier(), context.getResponse(), context.getError());
		for (Tag tag : tags) {
			keyValues = keyValues.and(tag.getKey(), tag.getValue());
		}
		return keyValues;
	}

	/*
	 * {@link WebClientExchangeTagsProvider} relies on a request attribute to get the URI
	 * template, we need to adapt to that.
	 */
	private static void mutateClientRequest(ClientObservationContext context) {
		ClientRequest clientRequest = ClientRequest.from(context.getCarrier())
				.attribute(URI_TEMPLATE_ATTRIBUTE, context.getUriTemplate()).build();
		context.setCarrier(clientRequest);
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(ClientObservationContext context) {
		return KeyValues.empty();
	}

	@Override
	public String getName() {
		return this.metricName;
	}

}
