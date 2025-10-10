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

package org.springframework.boot.http.client;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpClientSettings}.
 *
 * @author Phillip Webb
 */
class HttpClientSettingsTests {

	private static final Duration ONE_SECOND = Duration.ofSeconds(1);

	private static final Duration TWO_SECONDS = Duration.ofSeconds(2);

	@Test
	void defaults() {
		HttpClientSettings settings = HttpClientSettings.defaults();
		assertThat(settings.redirects()).isNull();
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void createWithNulls() {
		HttpClientSettings settings = new HttpClientSettings(null, null, null, null);
		assertThat(settings.redirects()).isNull();
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void withConnectTimeoutReturnsInstanceWithUpdatedConnectionTimeout() {
		HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(ONE_SECOND);
		assertThat(settings.redirects()).isNull();
		assertThat(settings.connectTimeout()).isEqualTo(ONE_SECOND);
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void withReadTimeoutReturnsInstanceWithUpdatedReadTimeout() {
		HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(ONE_SECOND);
		assertThat(settings.redirects()).isNull();
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isEqualTo(ONE_SECOND);
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void withSslBundleReturnsInstanceWithUpdatedSslBundle() {
		SslBundle sslBundle = mock(SslBundle.class);
		HttpClientSettings settings = HttpClientSettings.defaults().withSslBundle(sslBundle);
		assertThat(settings.redirects()).isNull();
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isSameAs(sslBundle);
	}

	@Test
	void withRedirectsReturnsInstanceWithUpdatedRedirect() {
		HttpClientSettings settings = HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
		assertThat(settings.redirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isNull();
	}

	@Test
	void orElseReturnsNewInstanceWithUpdatedValues() {
		SslBundle sslBundle = mock(SslBundle.class);
		HttpClientSettings settings = new HttpClientSettings(null, ONE_SECOND, null, null)
			.orElse(new HttpClientSettings(HttpRedirects.FOLLOW_WHEN_POSSIBLE, TWO_SECONDS, TWO_SECONDS, sslBundle));
		assertThat(settings.redirects()).isEqualTo(HttpRedirects.FOLLOW_WHEN_POSSIBLE);
		assertThat(settings.connectTimeout()).isEqualTo(ONE_SECOND);
		assertThat(settings.readTimeout()).isEqualTo(TWO_SECONDS);
		assertThat(settings.sslBundle()).isEqualTo(sslBundle);
	}

	@Test
	void ofSslBundleCreatesNewSettings() {
		SslBundle sslBundle = mock(SslBundle.class);
		HttpClientSettings settings = HttpClientSettings.ofSslBundle(sslBundle);
		assertThat(settings.redirects()).isNull();
		assertThat(settings.connectTimeout()).isNull();
		assertThat(settings.readTimeout()).isNull();
		assertThat(settings.sslBundle()).isSameAs(sslBundle);
	}

}
