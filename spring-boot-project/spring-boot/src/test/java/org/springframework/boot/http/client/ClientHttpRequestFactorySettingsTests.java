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

package org.springframework.boot.http.client;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClientHttpRequestFactorySettings}.
 *
 * @author Phillip Webb
 */
class ClientHttpRequestFactorySettingsTests {

	private static final Duration ONE_SECOND = Duration.ofSeconds(1);

	@Test
	void defaults() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults();
		assertThat(settings.redirects()).isEqualTo(Redirects.FOLLOW_WHEN_POSSIBLE);
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void createWithNullsUsesDefaults() {
		ClientHttpRequestFactorySettings settings = new ClientHttpRequestFactorySettings(null, null, null, null);
		assertThat(settings.redirects()).isEqualTo(Redirects.FOLLOW_WHEN_POSSIBLE);
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void withConnectTimeoutReturnsInstanceWithUpdatedConnectionTimeout() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(ONE_SECOND);
		assertThat(settings.connectTimeout()).isEqualTo(ONE_SECOND);
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void withReadTimeoutReturnsInstanceWithUpdatedReadTimeout() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withReadTimeout(ONE_SECOND);
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isEqualTo(ONE_SECOND);
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void withSslBundleReturnsInstanceWithUpdatedSslBundle() {
		SslBundle sslBundle = mock(SslBundle.class);
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withSslBundle(sslBundle);
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isSameAs(sslBundle);
	}

	@Test
	void withRedirectsReturnsInstanceWithUpdatedRedirect() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withRedirects(Redirects.DONT_FOLLOW);
		assertThat(settings.redirects()).isEqualTo(Redirects.DONT_FOLLOW);
	}

}
