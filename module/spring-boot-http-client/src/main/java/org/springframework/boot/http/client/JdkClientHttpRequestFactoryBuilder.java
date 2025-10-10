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

import java.net.http.HttpClient;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#jdk()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Sangmin Park
 * @since 3.4.0
 */
public final class JdkClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<JdkClientHttpRequestFactory> {

	private final JdkHttpClientBuilder httpClientBuilder;

	JdkClientHttpRequestFactoryBuilder() {
		this(null, new JdkHttpClientBuilder());
	}

	private JdkClientHttpRequestFactoryBuilder(@Nullable List<Consumer<JdkClientHttpRequestFactory>> customizers,
			JdkHttpClientBuilder httpClientBuilder) {
		super(customizers);
		this.httpClientBuilder = httpClientBuilder;
	}

	@Override
	public JdkClientHttpRequestFactoryBuilder withCustomizer(Consumer<JdkClientHttpRequestFactory> customizer) {
		return new JdkClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
	}

	@Override
	public JdkClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<JdkClientHttpRequestFactory>> customizers) {
		return new JdkClientHttpRequestFactoryBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
	}

	/**
	 * Return a new {@link JdkClientHttpRequestFactoryBuilder} that uses the given
	 * executor with the underlying {@link java.net.http.HttpClient.Builder}.
	 * @param executor the executor to use
	 * @return a new {@link JdkClientHttpRequestFactoryBuilder} instance
	 * @since 4.0.0
	 */
	public JdkClientHttpRequestFactoryBuilder withExecutor(Executor executor) {
		return new JdkClientHttpRequestFactoryBuilder(getCustomizers(), this.httpClientBuilder.withExecutor(executor));
	}

	/**
	 * Return a new {@link JdkClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link java.net.http.HttpClient.Builder}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link JdkClientHttpRequestFactoryBuilder} instance
	 */
	public JdkClientHttpRequestFactoryBuilder withHttpClientCustomizer(
			Consumer<HttpClient.Builder> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new JdkClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withCustomizer(httpClientCustomizer));
	}

	/**
	 * Return a new {@link JdkClientHttpRequestFactoryBuilder} that applies the given
	 * customizer. This can be useful for applying pre-packaged customizations.
	 * @param customizer the customizer to apply
	 * @return a new {@link JdkClientHttpRequestFactoryBuilder}
	 * @since 4.0.0
	 */
	public JdkClientHttpRequestFactoryBuilder with(UnaryOperator<JdkClientHttpRequestFactoryBuilder> customizer) {
		return customizer.apply(this);
	}

	@Override
	protected JdkClientHttpRequestFactory createClientHttpRequestFactory(HttpClientSettings settings) {
		HttpClient httpClient = this.httpClientBuilder.build(settings.withReadTimeout(null));
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		PropertyMapper map = PropertyMapper.get();
		map.from(settings::readTimeout).to(requestFactory::setReadTimeout);
		return requestFactory;
	}

	static class Classes {

		static final String HTTP_CLIENT = "java.net.http.HttpClient";

		static boolean present(@Nullable ClassLoader classLoader) {
			return ClassUtils.isPresent(HTTP_CLIENT, classLoader);
		}

	}

}
