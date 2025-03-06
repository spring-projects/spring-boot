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

package org.springframework.boot.autoconfigure.http.client.reactive;

import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.http.client.AbstractHttpClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.http.client.reactive.ClientHttpConnector;

/**
 * Base {@link ConfigurationProperties @ConfigurationProperties} for configuring a
 * {@link ClientHttpConnector}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 * @see ClientHttpConnectorSettings
 */
public abstract class AbstractClientHttpConnectorProperties extends AbstractHttpClientProperties {

	/**
	 * Default connector used for a client HTTP request.
	 */
	private Connector connector;

	public Connector getConnector() {
		return this.connector;
	}

	public void setConnector(Connector connector) {
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
