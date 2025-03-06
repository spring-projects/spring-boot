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

package org.springframework.boot.autoconfigure.http.client;

import java.time.Duration;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;

/**
 * Abstract base class for properties that directly or indirectly make use of a blocking
 * or reactive HTTP client.
 *
 * @author Phillip Webb
 * @since 3.5.0
 * @see HttpClientSettings
 */
public abstract class AbstractHttpClientProperties {

	/**
	 * Handling for HTTP redirects.
	 */
	private HttpRedirects redirects;

	/**
	 * Default connect timeout for a client HTTP request.
	 */
	private Duration connectTimeout;

	/**
	 * Default read timeout for a client HTTP request.
	 */
	private Duration readTimeout;

	/**
	 * Default SSL configuration for a client HTTP request.
	 */
	private final Ssl ssl = new Ssl();

	public HttpRedirects getRedirects() {
		return this.redirects;
	}

	public void setRedirects(HttpRedirects redirects) {
		this.redirects = redirects;
	}

	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
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
		private String bundle;

		public String getBundle() {
			return this.bundle;
		}

		public void setBundle(String bundle) {
			this.bundle = bundle;
		}

	}

}
