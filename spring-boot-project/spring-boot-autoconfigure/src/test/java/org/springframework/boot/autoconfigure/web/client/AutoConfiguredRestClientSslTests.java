/*
 * Copyright 2012-2025 the original author or authors.
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

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfiguredRestClientSsl}.
 *
 * @author Dmytro Nosan
 */
@ExtendWith(MockitoExtension.class)
class AutoConfiguredRestClientSslTests {

	private final ClientHttpRequestFactorySettings clientHttpRequestFactorySettings = ClientHttpRequestFactorySettings
		.ofSslBundle(mock(SslBundle.class, "Default SslBundle"))
		.withRedirects(Redirects.DONT_FOLLOW)
		.withReadTimeout(Duration.ofSeconds(10))
		.withConnectTimeout(Duration.ofSeconds(30));

	@Mock
	private SslBundles sslBundles;

	@Mock
	private ClientHttpRequestFactoryBuilder<ClientHttpRequestFactory> clientHttpRequestFactoryBuilder;

	@Mock
	private ClientHttpRequestFactory clientHttpRequestFactory;

	@Test
	void shouldConfigureRestClientUsingBundleName() {
		String bundleName = "test";
		SslBundle sslBundle = mock(SslBundle.class, "SslBundle named '%s'".formatted(bundleName));

		given(this.sslBundles.getBundle(bundleName)).willReturn(sslBundle);
		given(this.clientHttpRequestFactoryBuilder
			.build(this.clientHttpRequestFactorySettings.withSslBundle(sslBundle)))
			.willReturn(this.clientHttpRequestFactory);

		assertThat(applySslBundle((restClientSsl) -> restClientSsl.fromBundle(bundleName)))
			.hasFieldOrPropertyWithValue("clientRequestFactory", this.clientHttpRequestFactory);

	}

	@Test
	void shouldConfigureRestClientUsingBundle() {
		SslBundle sslBundle = mock(SslBundle.class, "Custom SslBundle");

		given(this.clientHttpRequestFactoryBuilder
			.build(this.clientHttpRequestFactorySettings.withSslBundle(sslBundle)))
			.willReturn(this.clientHttpRequestFactory);

		assertThat(applySslBundle((restClientSsl) -> restClientSsl.fromBundle(sslBundle)))
			.hasFieldOrPropertyWithValue("clientRequestFactory", this.clientHttpRequestFactory);
	}

	private RestClient applySslBundle(Function<RestClientSsl, Consumer<Builder>> applySslBundle) {
		Builder builder = RestClient.builder();
		applySslBundle.apply(getRestClientSsl()).accept(builder);
		return builder.build();
	}

	private RestClientSsl getRestClientSsl() {
		return new AutoConfiguredRestClientSsl(this.clientHttpRequestFactoryBuilder,
				this.clientHttpRequestFactorySettings, this.sslBundles);
	}

}
