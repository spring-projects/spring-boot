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

import java.util.function.Supplier;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Base {@link ConfigurationProperties @ConfigurationProperties} for configuring a
 * {@link ClientHttpRequestFactory}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 * @see ClientHttpRequestFactorySettings
 */
public abstract class AbstractHttpRequestFactoryProperties extends AbstractHttpClientProperties {

	/**
	 * Default factory used for a client HTTP request.
	 */
	private Factory factory;

	public Factory getFactory() {
		return this.factory;
	}

	public void setFactory(Factory factory) {
		this.factory = factory;
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
