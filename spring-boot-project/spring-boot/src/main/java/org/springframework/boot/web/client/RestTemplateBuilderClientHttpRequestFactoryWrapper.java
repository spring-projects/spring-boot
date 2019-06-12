/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.client;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * {@link ClientHttpRequestFactory} to apply customizations from the
 * {@link RestTemplateBuilder}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 */
class RestTemplateBuilderClientHttpRequestFactoryWrapper extends AbstractClientHttpRequestFactoryWrapper {

	private final BasicAuthentication basicAuthentication;

	private final Map<String, String> defaultHeaders;

	private final Set<RestTemplateRequestCustomizer<?>> requestCustomizers;

	RestTemplateBuilderClientHttpRequestFactoryWrapper(ClientHttpRequestFactory requestFactory,
			BasicAuthentication basicAuthentication, Map<String, String> defaultHeaders,
			Set<RestTemplateRequestCustomizer<?>> requestCustomizers) {
		super(requestFactory);
		this.basicAuthentication = basicAuthentication;
		this.defaultHeaders = defaultHeaders;
		this.requestCustomizers = requestCustomizers;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod, ClientHttpRequestFactory requestFactory)
			throws IOException {
		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		HttpHeaders headers = request.getHeaders();
		if (this.basicAuthentication != null) {
			this.basicAuthentication.applyTo(headers);
		}
		this.defaultHeaders.forEach(headers::addIfAbsent);
		LambdaSafe.callbacks(RestTemplateRequestCustomizer.class, this.requestCustomizers, request)
				.invoke((customizer) -> customizer.customize(request));
		return request;
	}

}
