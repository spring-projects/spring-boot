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

package org.springframework.boot.http.client.reactive;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import reactor.netty.http.client.HttpClient;

import org.springframework.boot.http.client.ReactorHttpClientBuilder;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpConnectorBuilder#reactor()}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class ReactorClientHttpConnectorBuilder
		extends AbstractClientHttpConnectorBuilder<ReactorClientHttpConnector> {

	private final ReactorHttpClientBuilder httpClientBuilder;

	ReactorClientHttpConnectorBuilder() {
		this(null, new ReactorHttpClientBuilder());
	}

	private ReactorClientHttpConnectorBuilder(List<Consumer<ReactorClientHttpConnector>> customizers,
			ReactorHttpClientBuilder httpClientBuilder) {
		super(customizers);
		this.httpClientBuilder = httpClientBuilder;
	}

	@Override
	public ReactorClientHttpConnectorBuilder withCustomizer(Consumer<ReactorClientHttpConnector> customizer) {
		return new ReactorClientHttpConnectorBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
	}

	@Override
	public ReactorClientHttpConnectorBuilder withCustomizers(
			Collection<Consumer<ReactorClientHttpConnector>> customizers) {
		return new ReactorClientHttpConnectorBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
	}

	/**
	 * Return a new {@link ReactorClientHttpConnectorBuilder} that uses the given
	 * {@link ReactorResourceFactory} to create the underlying {@link HttpClient}.
	 * @param reactorResourceFactory the {@link ReactorResourceFactory} to use
	 * @return a new {@link ReactorClientHttpConnectorBuilder} instance
	 */
	public ReactorClientHttpConnectorBuilder withReactorResourceFactory(ReactorResourceFactory reactorResourceFactory) {
		Assert.notNull(reactorResourceFactory, "'reactorResourceFactory' must not be null");
		return new ReactorClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withReactorResourceFactory(reactorResourceFactory));
	}

	/**
	 * Return a new {@link ReactorClientHttpConnectorBuilder} that uses the given factory
	 * to create the underlying {@link HttpClient}.
	 * @param factory the factory to use
	 * @return a new {@link ReactorClientHttpConnectorBuilder} instance
	 */
	public ReactorClientHttpConnectorBuilder withHttpClientFactory(Supplier<HttpClient> factory) {
		Assert.notNull(factory, "'factory' must not be null");
		return new ReactorClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withHttpClientFactory(factory));
	}

	/**
	 * Return a new {@link ReactorClientHttpConnectorBuilder} that applies additional
	 * customization to the underlying {@link HttpClient}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link ReactorClientHttpConnectorBuilder} instance
	 */
	public ReactorClientHttpConnectorBuilder withHttpClientCustomizer(UnaryOperator<HttpClient> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new ReactorClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withHttpClientCustomizer(httpClientCustomizer));
	}

	@Override
	protected ReactorClientHttpConnector createClientHttpConnector(ClientHttpConnectorSettings settings) {
		HttpClient httpClient = this.httpClientBuilder.build(asHttpClientSettings(settings));
		return new ReactorClientHttpConnector(httpClient);
	}

	static class Classes {

		static final String HTTP_CLIENT = "reactor.netty.http.client.HttpClient";

		static boolean present(ClassLoader classLoader) {
			return ClassUtils.isPresent(HTTP_CLIENT, classLoader);
		}

	}

}
