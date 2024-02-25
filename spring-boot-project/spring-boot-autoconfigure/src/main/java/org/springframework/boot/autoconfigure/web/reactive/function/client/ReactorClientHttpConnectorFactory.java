/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.SslContextSpec;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.function.ThrowingConsumer;

/**
 * {@link ClientHttpConnectorFactory} for {@link ReactorClientHttpConnector}.
 *
 * @author Phillip Webb
 * @author Fernando Cappi
 */
class ReactorClientHttpConnectorFactory implements ClientHttpConnectorFactory<ReactorClientHttpConnector> {

	private final ReactorResourceFactory reactorResourceFactory;

	private final Supplier<Stream<ReactorNettyHttpClientMapper>> mappers;

	/**
	 * Constructs a new ReactorClientHttpConnectorFactory with the specified
	 * ReactorResourceFactory and empty Stream.
	 * @param reactorResourceFactory the ReactorResourceFactory to be used by the
	 * ReactorClientHttpConnectorFactory
	 */
	ReactorClientHttpConnectorFactory(ReactorResourceFactory reactorResourceFactory) {
		this(reactorResourceFactory, Stream::empty);
	}

	/**
	 * Constructs a new ReactorClientHttpConnectorFactory with the specified
	 * ReactorResourceFactory and mappers.
	 * @param reactorResourceFactory the ReactorResourceFactory used to create
	 * ReactorNettyHttpClient instances
	 * @param mappers the supplier of a stream of ReactorNettyHttpClientMapper instances
	 * used to map ReactorNettyHttpClient instances to specific HTTP clients
	 */
	ReactorClientHttpConnectorFactory(ReactorResourceFactory reactorResourceFactory,
			Supplier<Stream<ReactorNettyHttpClientMapper>> mappers) {
		this.reactorResourceFactory = reactorResourceFactory;
		this.mappers = mappers;
	}

	/**
	 * Creates a ReactorClientHttpConnector with the provided SSL bundle.
	 * @param sslBundle the SSL bundle to configure the connector with, can be null
	 * @return the created ReactorClientHttpConnector
	 */
	@Override
	public ReactorClientHttpConnector createClientHttpConnector(SslBundle sslBundle) {
		List<ReactorNettyHttpClientMapper> mappers = this.mappers.get()
			.collect(Collectors.toCollection(ArrayList::new));
		if (sslBundle != null) {
			mappers.add(new SslConfigurer(sslBundle));
		}
		return new ReactorClientHttpConnector(this.reactorResourceFactory,
				ReactorNettyHttpClientMapper.of(mappers)::configure);
	}

	/**
	 * Configures the Netty {@link HttpClient} with SSL.
	 */
	private static class SslConfigurer implements ReactorNettyHttpClientMapper {

		private final SslBundle sslBundle;

		/**
		 * Constructs a new SslConfigurer with the specified SslBundle.
		 * @param sslBundle the SslBundle containing the SSL configuration details
		 */
		SslConfigurer(SslBundle sslBundle) {
			this.sslBundle = sslBundle;
		}

		/**
		 * Configures the provided HttpClient with SSL customization.
		 * @param httpClient the HttpClient to be configured
		 * @return the configured HttpClient
		 * @throws IllegalStateException if an error occurs while customizing SSL
		 */
		@Override
		public HttpClient configure(HttpClient httpClient) {
			return httpClient.secure(ThrowingConsumer.of(this::customizeSsl).throwing(IllegalStateException::new));
		}

		/**
		 * Customizes the SSL configuration for the specified SslContextSpec.
		 * @param spec the SslContextSpec to customize
		 * @throws SSLException if an error occurs while building the SSL context
		 */
		private void customizeSsl(SslContextSpec spec) throws SSLException {
			SslOptions options = this.sslBundle.getOptions();
			SslManagerBundle managers = this.sslBundle.getManagers();
			SslContextBuilder builder = SslContextBuilder.forClient()
				.keyManager(managers.getKeyManagerFactory())
				.trustManager(managers.getTrustManagerFactory())
				.ciphers(SslOptions.asSet(options.getCiphers()))
				.protocols(options.getEnabledProtocols());
			spec.sslContext(builder.build());
		}

	}

}
