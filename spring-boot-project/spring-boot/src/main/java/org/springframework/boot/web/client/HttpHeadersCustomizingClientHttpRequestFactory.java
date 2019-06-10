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
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequestFactory} to apply default headers to a request unless header
 * values were provided.
 *
 * @author Ilya Lukyanovich
 * @author Dmytro Nosan
 */
class HttpHeadersCustomizingClientHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {

	private final Collection<? extends HttpHeadersCustomizer> customizers;

	HttpHeadersCustomizingClientHttpRequestFactory(Collection<? extends HttpHeadersCustomizer> customizers,
			ClientHttpRequestFactory clientHttpRequestFactory) {
		super(clientHttpRequestFactory);
		Assert.notEmpty(customizers, "Customizers must not be empty");
		this.customizers = customizers;
	}

	@NotNull
	@Override
	protected ClientHttpRequest createRequest(@NotNull URI uri, @NotNull HttpMethod httpMethod,
			ClientHttpRequestFactory requestFactory) throws IOException {
		ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
		this.customizers.forEach((customizer) -> customizer.applyTo(request.getHeaders()));
		return request;
	}

}
