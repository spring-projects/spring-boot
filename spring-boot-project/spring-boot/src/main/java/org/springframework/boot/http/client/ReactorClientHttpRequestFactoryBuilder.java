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

package org.springframework.boot.http.client;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import reactor.netty.http.client.HttpClient;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#reactor()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.4.0
 */
public final class ReactorClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<ReactorClientHttpRequestFactory> {

	private final ReactorHttpClientBuilder httpClientBuilder;

	ReactorClientHttpRequestFactoryBuilder() {
		this(null, new ReactorHttpClientBuilder());
	}

	private ReactorClientHttpRequestFactoryBuilder(List<Consumer<ReactorClientHttpRequestFactory>> customizers,
			ReactorHttpClientBuilder httpClientBuilder) {
		super(customizers);
		this.httpClientBuilder = httpClientBuilder;
	}

	@Override
	public ReactorClientHttpRequestFactoryBuilder withCustomizer(Consumer<ReactorClientHttpRequestFactory> customizer) {
		return new ReactorClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
	}

	@Override
	public ReactorClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<ReactorClientHttpRequestFactory>> customizers) {
		return new ReactorClientHttpRequestFactoryBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
	}

	/**
	 * Return a new {@link ReactorClientHttpRequestFactoryBuilder} that uses the given
	 * {@link ReactorResourceFactory} to create the underlying {@link HttpClient}.
	 * @param reactorResourceFactory the {@link ReactorResourceFactory} to use
	 * @return a new {@link ReactorClientHttpRequestFactoryBuilder} instance
	 * @since 3.5.0
	 */
	public ReactorClientHttpRequestFactoryBuilder withReactorResourceFactory(
			ReactorResourceFactory reactorResourceFactory) {
		Assert.notNull(reactorResourceFactory, "'reactorResourceFactory' must not be null");
		return new ReactorClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withReactorResourceFactory(reactorResourceFactory));
	}

	/**
	 * Return a new {@link ReactorClientHttpRequestFactoryBuilder} that uses the given
	 * factory to create the underlying {@link HttpClient}.
	 * @param factory the factory to use
	 * @return a new {@link ReactorClientHttpRequestFactoryBuilder} instance
	 * @since 3.5.0
	 */
	public ReactorClientHttpRequestFactoryBuilder withHttpClientFactory(Supplier<HttpClient> factory) {
		Assert.notNull(factory, "'factory' must not be null");
		return new ReactorClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withHttpClientFactory(factory));
	}

	/**
	 * Return a new {@link ReactorClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link HttpClient}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link ReactorClientHttpRequestFactoryBuilder} instance
	 */
	public ReactorClientHttpRequestFactoryBuilder withHttpClientCustomizer(
			UnaryOperator<HttpClient> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new ReactorClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withHttpClientCustomizer(httpClientCustomizer));
	}

	@Override
	protected ReactorClientHttpRequestFactory createClientHttpRequestFactory(
			ClientHttpRequestFactorySettings settings) {
		HttpClient httpClient = this.httpClientBuilder.build(asHttpClientSettings(settings.withTimeouts(null, null)));
		ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(httpClient);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
		map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
		return requestFactory;
	}

	static class Classes {

		static final String HTTP_CLIENT = "reactor.netty.http.client.HttpClient";

		static boolean present(ClassLoader classLoader) {
			return ClassUtils.isPresent(HTTP_CLIENT, classLoader);
		}

	}

}
