/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.restclient.autoconfigure;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClientCustomizer} to apply {@link HttpMessageConverter
 * HttpMessageConverters}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
public class HttpMessageConvertersRestClientCustomizer implements RestClientCustomizer {

	private final List<ClientHttpMessageConvertersCustomizer> customizers;

	public HttpMessageConvertersRestClientCustomizer(ClientHttpMessageConvertersCustomizer... customizers) {
		this(Arrays.asList(customizers));
	}

	public HttpMessageConvertersRestClientCustomizer(List<ClientHttpMessageConvertersCustomizer> customizers) {
		Assert.notNull(customizers, "customizers must not be null");
		this.customizers = customizers;
	}

	@Override
	public void customize(RestClient.Builder restClientBuilder) {
		restClientBuilder.configureMessageConverters(
				(builder) -> this.customizers.forEach((customizer) -> customizer.customize(builder)));
	}

}
