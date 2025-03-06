/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.function.Consumer;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * An auto-configured {@link RestClientSsl} implementation.
 *
 * @author Phillip Webb
 */
class AutoConfiguredRestClientSsl implements RestClientSsl {

	private final ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder;

	private final SslBundles sslBundles;

	AutoConfiguredRestClientSsl(ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder,
			SslBundles sslBundles) {
		this.clientHttpRequestFactoryBuilder = clientHttpRequestFactoryBuilder;
		this.sslBundles = sslBundles;
	}

	@Override
	public Consumer<RestClient.Builder> fromBundle(String bundleName) {
		return fromBundle(this.sslBundles.getBundle(bundleName));
	}

	@Override
	public Consumer<RestClient.Builder> fromBundle(SslBundle bundle) {
		return (builder) -> {
			ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.ofSslBundle(bundle);
			ClientHttpRequestFactory requestFactory = this.clientHttpRequestFactoryBuilder.build(settings);
			builder.requestFactory(requestFactory);
		};
	}

}
