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

package org.springframework.boot.http.client.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpClientSettingsPropertyMapper}.
 *
 * @author Steve Armstrong
 */
class HttpClientSettingsPropertyMapperTests {

	@Test
	void mapWhenPropertiesIsNullAndBaseSettingsIsNullReturnsDefaults() {
		HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
		HttpClientSettings result = mapper.map(null);
		assertThat(result).isEqualTo(HttpClientSettings.defaults());
	}

	@Test
	void mapWhenPropertiesIsNullReturnsBaseSettings() {
		HttpClientSettings baseSettings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(10));
		HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, baseSettings);
		HttpClientSettings result = mapper.map(null);
		assertThat(result).isEqualTo(baseSettings);
	}

	@Test
	void mapMapsRedirects() {
		HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
		TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
		properties.setRedirects(HttpRedirects.DONT_FOLLOW);
		HttpClientSettings result = mapper.map(properties);
		assertThat(result.redirects()).isEqualTo(HttpRedirects.DONT_FOLLOW);
	}

	@Test
	void mapMapsConnectTimeout() {
		HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
		TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
		properties.setConnectTimeout(Duration.ofSeconds(5));
		HttpClientSettings result = mapper.map(properties);
		assertThat(result.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
	}

	@Test
	void mapMapsReadTimeout() {
		HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
		TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
		properties.setReadTimeout(Duration.ofSeconds(30));
		HttpClientSettings result = mapper.map(properties);
		assertThat(result.readTimeout()).isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void mapMapsSslBundle() {
		SslBundle sslBundle = mock(SslBundle.class);
		SslBundles sslBundles = mock(SslBundles.class);
		given(sslBundles.getBundle("test-bundle")).willReturn(sslBundle);
		HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(sslBundles, null);
		TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
		properties.getSsl().setBundle("test-bundle");
		HttpClientSettings result = mapper.map(properties);
		assertThat(result.sslBundle()).isSameAs(sslBundle);
	}

	@Test
	void mapUsesBaseSettingsForMissingProperties() {
		HttpClientSettings baseSettings = new HttpClientSettings(HttpRedirects.FOLLOW_WHEN_POSSIBLE,
				Duration.ofSeconds(15), Duration.ofSeconds(25), null);
		HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, baseSettings);
		TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
		properties.setConnectTimeout(Duration.ofSeconds(5));
		HttpClientSettings result = mapper.map(properties);
		assertThat(result.redirects()).isEqualTo(HttpRedirects.FOLLOW_WHEN_POSSIBLE);
		assertThat(result.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
		assertThat(result.readTimeout()).isEqualTo(Duration.ofSeconds(25));
	}

	@Test
	void mapWhenSslBundleRequestedButSslBundlesIsNullThrowsException() {
		HttpClientSettingsPropertyMapper mapper = new HttpClientSettingsPropertyMapper(null, null);
		TestHttpClientSettingsProperties properties = new TestHttpClientSettingsProperties();
		properties.getSsl().setBundle("test-bundle");
		assertThatIllegalStateException().isThrownBy(() -> mapper.map(properties))
			.withMessage("No 'sslBundles' available");
	}

	static class TestHttpClientSettingsProperties extends HttpClientSettingsProperties {

	}

}
