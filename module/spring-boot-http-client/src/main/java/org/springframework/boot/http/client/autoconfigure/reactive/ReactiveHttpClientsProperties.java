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

package org.springframework.boot.http.client.autoconfigure.reactive;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientsProperties;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} to configure the defaults used
 * for reactive HTTP clients.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see HttpClientsProperties
 */
@ConfigurationProperties("spring.http.clients.reactive")
public class ReactiveHttpClientsProperties {

	/**
	 * Default connector used for a client HTTP request.
	 */
	private @Nullable Connector connector;

	public @Nullable Connector getConnector() {
		return this.connector;
	}

	public void setConnector(@Nullable Connector connector) {
		this.connector = connector;
	}

	/**
	 * Supported factory types.
	 */
	public enum Connector {

		/**
		 * Reactor-Netty.
		 */
		REACTOR(ClientHttpConnectorBuilder::reactor),

		/**
		 * Jetty's HttpClient.
		 */
		JETTY(ClientHttpConnectorBuilder::jetty),

		/**
		 * Apache HttpComponents HttpClient.
		 */
		HTTP_COMPONENTS(ClientHttpConnectorBuilder::httpComponents),

		/**
		 * Java's HttpClient.
		 */
		JDK(ClientHttpConnectorBuilder::jdk);

		private final Supplier<ClientHttpConnectorBuilder<?>> builderSupplier;

		Connector(Supplier<ClientHttpConnectorBuilder<?>> builderSupplier) {
			this.builderSupplier = builderSupplier;
		}

		ClientHttpConnectorBuilder<?> builder() {
			return this.builderSupplier.get();
		}

	}

}
