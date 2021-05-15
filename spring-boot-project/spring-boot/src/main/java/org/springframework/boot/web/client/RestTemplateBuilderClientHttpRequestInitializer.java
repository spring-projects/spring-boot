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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInitializer;

/**
 * {@link ClientHttpRequestFactory} to apply customizations from the
 * {@link RestTemplateBuilder}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 */
class RestTemplateBuilderClientHttpRequestInitializer implements ClientHttpRequestInitializer {

	private final BasicAuthentication basicAuthentication;

	private final Map<String, List<String>> defaultHeaders;

	private final Set<RestTemplateRequestCustomizer<?>> requestCustomizers;

	RestTemplateBuilderClientHttpRequestInitializer(BasicAuthentication basicAuthentication,
			Map<String, List<String>> defaultHeaders, Set<RestTemplateRequestCustomizer<?>> requestCustomizers) {
		this.basicAuthentication = basicAuthentication;
		this.defaultHeaders = defaultHeaders;
		this.requestCustomizers = requestCustomizers;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initialize(ClientHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		if (this.basicAuthentication != null) {
			this.basicAuthentication.applyTo(headers);
		}
		this.defaultHeaders.forEach(headers::putIfAbsent);
		LambdaSafe.callbacks(RestTemplateRequestCustomizer.class, this.requestCustomizers, request)
				.invoke((customizer) -> customizer.customize(request));
	}

}
