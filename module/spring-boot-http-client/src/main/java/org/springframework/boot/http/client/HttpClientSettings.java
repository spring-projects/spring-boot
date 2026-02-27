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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;

/**
 * Settings that can be applied when creating an imperative or reactive HTTP client.
 *
 * @param cookies the cookie handling strategy to use or null to use the underlying
 * library's default
 * @param redirects the follow redirect strategy to use or null to redirect whenever the
 * underlying library allows it
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param sslBundle the SSL bundle providing SSL configuration
 * @author Phillip Webb
 * @since 3.5.0
 */
public record HttpClientSettings(@Nullable HttpCookies cookies, @Nullable HttpRedirects redirects,
		@Nullable Duration connectTimeout, @Nullable Duration readTimeout, @Nullable SslBundle sslBundle) {

	/**
	 * Create a new {@link HttpClientSettings} instance.
	 * @param redirects the follow redirect strategy to use
	 * @param connectTimeout the connect timeout
	 * @param readTimeout the read timeout
	 * @param sslBundle the SSL bundle providing SSL configuration
	 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of
	 * {@link HttpClientSettings#HttpClientSettings(HttpCookies, HttpRedirects, Duration, Duration, SslBundle)}
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public HttpClientSettings(@Nullable HttpRedirects redirects, @Nullable Duration connectTimeout,
			@Nullable Duration readTimeout, @Nullable SslBundle sslBundle) {
		this(null, redirects, connectTimeout, readTimeout, sslBundle);
	}

	private static final HttpClientSettings defaults = new HttpClientSettings(null, null, null, null, null);

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated cookie handling
	 * setting.
	 * @param cookies the new cookie handling setting
	 * @return a new {@link HttpClientSettings} instance
	 * @since 4.1.0
	 */
	public HttpClientSettings withCookies(@Nullable HttpCookies cookies) {
		return new HttpClientSettings(cookies, this.redirects, this.connectTimeout, this.readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated connect timeout
	 * setting.
	 * @param connectTimeout the new connect timeout setting
	 * @return a new {@link HttpClientSettings} instance
	 * @since 4.0.0
	 */
	public HttpClientSettings withConnectTimeout(@Nullable Duration connectTimeout) {
		return new HttpClientSettings(this.cookies, this.redirects, connectTimeout, this.readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated read timeout
	 * setting.
	 * @param readTimeout the new read timeout setting
	 * @return a new {@link HttpClientSettings} instance
	 * @since 4.0.0
	 */
	public HttpClientSettings withReadTimeout(@Nullable Duration readTimeout) {
		return new HttpClientSettings(this.cookies, this.redirects, this.connectTimeout, readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated connect and read
	 * timeout setting.
	 * @param connectTimeout the new connect timeout setting
	 * @param readTimeout the new read timeout setting
	 * @return a new {@link HttpClientSettings} instance
	 * @since 4.0.0
	 */
	public HttpClientSettings withTimeouts(@Nullable Duration connectTimeout, @Nullable Duration readTimeout) {
		return new HttpClientSettings(this.cookies, this.redirects, connectTimeout, readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated SSL bundle
	 * setting.
	 * @param sslBundle the new SSL bundle setting
	 * @return a new {@link HttpClientSettings} instance
	 * @since 4.0.0
	 */
	public HttpClientSettings withSslBundle(@Nullable SslBundle sslBundle) {
		return new HttpClientSettings(this.cookies, this.redirects, this.connectTimeout, this.readTimeout, sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated redirect setting.
	 * @param redirects the new redirects setting
	 * @return a new {@link HttpClientSettings} instance
	 * @since 4.0.0
	 */
	public HttpClientSettings withRedirects(@Nullable HttpRedirects redirects) {
		return new HttpClientSettings(this.cookies, redirects, this.connectTimeout, this.readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance using values from this instance
	 * when they are present, or otherwise using values from {@code other}.
	 * @param other the settings to be used to obtain values not present in this instance
	 * @return a new {@link HttpClientSettings} instance
	 * @since 4.0.0
	 */
	public HttpClientSettings orElse(@Nullable HttpClientSettings other) {
		if (other == null) {
			return this;
		}
		HttpCookies cookies = (cookies() != null) ? cookies() : other.cookies();
		HttpRedirects redirects = (redirects() != null) ? redirects() : other.redirects();
		Duration connectTimeout = (connectTimeout() != null) ? connectTimeout() : other.connectTimeout();
		Duration readTimeout = (readTimeout() != null) ? readTimeout() : other.readTimeout();
		SslBundle sslBundle = (sslBundle() != null) ? sslBundle() : other.sslBundle();
		return new HttpClientSettings(cookies, redirects, connectTimeout, readTimeout, sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} using defaults for all settings other than
	 * the provided SSL bundle.
	 * @param sslBundle the SSL bundle setting
	 * @return a new {@link HttpClientSettings} instance
	 * @since 4.0.0
	 */
	public static HttpClientSettings ofSslBundle(@Nullable SslBundle sslBundle) {
		return defaults().withSslBundle(sslBundle);
	}

	/**
	 * Use defaults settings, which can differ depending on the implementation.
	 * @return default settings
	 * @since 4.0.0
	 */
	public static HttpClientSettings defaults() {
		return defaults;
	}

}
