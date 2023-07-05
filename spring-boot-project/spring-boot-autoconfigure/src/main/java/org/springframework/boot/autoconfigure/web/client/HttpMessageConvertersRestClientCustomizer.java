/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClientCustomizer} to apply {@link HttpMessageConverter
 * HttpMessageConverters}.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public class HttpMessageConvertersRestClientCustomizer implements RestClientCustomizer {

	private final Iterable<? extends HttpMessageConverter<?>> messageConverters;

	public HttpMessageConvertersRestClientCustomizer(HttpMessageConverter<?>... messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		this.messageConverters = Arrays.asList(messageConverters);
	}

	HttpMessageConvertersRestClientCustomizer(HttpMessageConverters messageConverters) {
		this.messageConverters = messageConverters;
	}

	@Override
	public void customize(RestClient.Builder restClientBuilder) {
		restClientBuilder.messageConverters(this::configureMessageConverters);
	}

	private void configureMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		if (this.messageConverters != null) {
			messageConverters.clear();
			this.messageConverters.forEach(messageConverters::add);
		}
	}

}
