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

import org.jspecify.annotations.Nullable;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;

/**
 * Base class for configuration properties configure {@link HttpClientSettings}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see HttpClientSettings
 */
public abstract class HttpClientSettingsProperties {

	/**
	 * Handling for HTTP redirects.
	 */
	private @Nullable HttpRedirects redirects;

	/**
	 * Default connect timeout for a client HTTP request.
	 */
	private @Nullable Duration connectTimeout;

	/**
	 * Default read timeout for a client HTTP request.
	 */
	private @Nullable Duration readTimeout;

	/**
	 * Default SSL configuration for a client HTTP request.
	 */
	private final Ssl ssl = new Ssl();

	public @Nullable HttpRedirects getRedirects() {
		return this.redirects;
	}

	public void setRedirects(@Nullable HttpRedirects redirects) {
		this.redirects = redirects;
	}

	public @Nullable Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(@Nullable Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public @Nullable Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(@Nullable Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	/**
	 * SSL configuration.
	 */
	public static class Ssl {

		/**
		 * SSL bundle to use.
		 */
		private @Nullable String bundle;

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

}
