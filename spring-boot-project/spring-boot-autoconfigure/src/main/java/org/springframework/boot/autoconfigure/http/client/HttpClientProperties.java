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

package org.springframework.boot.autoconfigure.http.client;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for a Spring's blocking HTTP
 * clients.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
@ConfigurationProperties("spring.http.client")
public class HttpClientProperties {

	/**
	 * Default factory used for a client HTTP request.
	 */
	private Factory factory;

	/**
	 * Handling for HTTP redirects.
	 */
	private Redirects redirects = Redirects.FOLLOW_WHEN_POSSIBLE;

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

	public Factory getFactory() {
		return this.factory;
	}

	public void setFactory(Factory factory) {
		this.factory = factory;
	}

	public Redirects getRedirects() {
		return this.redirects;
	}

	public void setRedirects(Redirects redirects) {
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
