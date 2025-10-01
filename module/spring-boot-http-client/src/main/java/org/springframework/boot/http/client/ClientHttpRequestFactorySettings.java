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
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Settings that can be applied when creating a {@link ClientHttpRequestFactory}.
 *
 * @param redirects the follow redirect strategy to use or null to redirect whenever the
 * underlying library allows it
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param sslBundle the SSL bundle providing SSL configuration
 * @param dnsResolver the DNS resolver to use
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 3.4.0
 * @see ClientHttpRequestFactoryBuilder
 */
public record ClientHttpRequestFactorySettings(HttpRedirects redirects, @Nullable Duration connectTimeout,
		@Nullable Duration readTimeout, @Nullable SslBundle sslBundle, @Nullable Object dnsResolver) {

	private static final ClientHttpRequestFactorySettings defaults = new ClientHttpRequestFactorySettings(null, null,
			null, null, null);

	public ClientHttpRequestFactorySettings(@Nullable HttpRedirects redirects, @Nullable Duration connectTimeout,
			@Nullable Duration readTimeout, @Nullable SslBundle sslBundle) {
		this(redirects, connectTimeout, readTimeout, sslBundle, null);
	}

	public ClientHttpRequestFactorySettings(@Nullable HttpRedirects redirects, @Nullable Duration connectTimeout,
			@Nullable Duration readTimeout, @Nullable SslBundle sslBundle, @Nullable Object dnsResolver) {
		this.redirects = (redirects != null) ? redirects : HttpRedirects.FOLLOW_WHEN_POSSIBLE;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.sslBundle = sslBundle;
		this.dnsResolver = dnsResolver;
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * connect timeout setting.
	 * @param connectTimeout the new connect timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withConnectTimeout(@Nullable Duration connectTimeout) {
		return new ClientHttpRequestFactorySettings(this.redirects, connectTimeout, this.readTimeout, this.sslBundle,
				this.dnsResolver);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated read
	 * timeout setting.
	 * @param readTimeout the new read timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withReadTimeout(@Nullable Duration readTimeout) {
		return new ClientHttpRequestFactorySettings(this.redirects, this.connectTimeout, readTimeout, this.sslBundle,
				this.dnsResolver);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * connect and read timeout setting.
	 * @param connectTimeout the new connect timeout setting
	 * @param readTimeout the new read timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withTimeouts(@Nullable Duration connectTimeout,
			@Nullable Duration readTimeout) {
		return new ClientHttpRequestFactorySettings(this.redirects, connectTimeout, readTimeout, this.sslBundle,
				this.dnsResolver);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated SSL
	 * bundle setting.
	 * @param sslBundle the new SSL bundle setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withSslBundle(@Nullable SslBundle sslBundle) {
		return new ClientHttpRequestFactorySettings(this.redirects, this.connectTimeout, this.readTimeout, sslBundle,
				this.dnsResolver);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated DNS
	 * resolver setting.
	 * @param dnsResolver the new DNS resolver setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withDnsResolver(@Nullable Object dnsResolver) {
		return new ClientHttpRequestFactorySettings(this.redirects, this.connectTimeout, this.readTimeout,
				this.sslBundle, dnsResolver);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * redirect setting.
	 * @param redirects the new redirects setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withRedirects(@Nullable HttpRedirects redirects) {
		return new ClientHttpRequestFactorySettings(redirects, this.connectTimeout, this.readTimeout, this.sslBundle,
				this.dnsResolver);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} using defaults for all
	 * settings other than the provided SSL bundle.
	 * @param sslBundle the SSL bundle setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public static ClientHttpRequestFactorySettings ofSslBundle(@Nullable SslBundle sslBundle) {
		return defaults().withSslBundle(sslBundle);
	}

	/**
	 * Use defaults for the {@link ClientHttpRequestFactory} which can differ depending on
	 * the implementation.
	 * @return default settings
	 */
	public static ClientHttpRequestFactorySettings defaults() {
		return defaults;
	}

}
