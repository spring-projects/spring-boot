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

package org.springframework.boot.http.client.autoconfigure.imperative;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.HttpClientsProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} to configure the defaults used
 * for imperative HTTP clients.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see HttpClientsProperties
 */
@ConfigurationProperties("spring.http.clients.imperative")
public class ImperativeHttpClientsProperties {

	/**
	 * Default factory used for a client HTTP request.
	 */
	private @Nullable Factory factory;

	public @Nullable Factory getFactory() {
		return this.factory;
	}

	public void setFactory(@Nullable Factory factory) {
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
