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
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesSource;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Base {@link ConfigurationProperties @ConfigurationProperties} for configuring a
 * {@link ClientHttpRequestFactory}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see ClientHttpRequestFactorySettings
 */
@ConfigurationPropertiesSource
public abstract class AbstractHttpRequestFactoryProperties {

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

	/**
	 * Default DNS configuration for a client HTTP request.
	 */
	private final Dns dns = new Dns();

	/**
	 * Default factory used for a client HTTP request.
	 */
	private @Nullable Factory factory;

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

	public Dns getDns() {
		return this.dns;
	}

	public @Nullable Factory getFactory() {
		return this.factory;
	}

	public void setFactory(@Nullable Factory factory) {
		this.factory = factory;
	}

	/**
	 * SSL configuration.
	 */
	@ConfigurationPropertiesSource
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

	/**
	 * DNS configuration.
	 */
	@ConfigurationPropertiesSource
	public static class Dns {

		/**
		 * Banned host.
		 */
		private @Nullable String banned;

		public @Nullable String getBanned() {
			return this.banned;
		}

		public void setBanned(@Nullable String banned) {
			this.banned = banned;
		}

	}

	/**
	 * Supported factory types.
	 */
	public enum Factory {

		/**
		 * Apache HttpComponents HttpClient.
		 */
		HTTP_COMPONENTS(ClientHttpRequestFactoryBuilder::httpComponents),

		/**
		 * Jetty's HttpClient.
		 */
		JETTY(ClientHttpRequestFactoryBuilder::jetty),

		/**
		 * Reactor-Netty.
		 */
		REACTOR(ClientHttpRequestFactoryBuilder::reactor),

		/**
		 * Java's HttpClient.
		 */
		JDK(ClientHttpRequestFactoryBuilder::jdk),

		/**
		 * Standard JDK facilities.
		 */
		SIMPLE(ClientHttpRequestFactoryBuilder::simple);

		private final Supplier<ClientHttpRequestFactoryBuilder<?>> builderSupplier;

		Factory(Supplier<ClientHttpRequestFactoryBuilder<?>> builderSupplier) {
			this.builderSupplier = builderSupplier;
		}

		ClientHttpRequestFactoryBuilder<?> builder() {
			return this.builderSupplier.get();
		}

	}

}
