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

package org.springframework.boot.http.client.reactive;

import java.net.http.HttpClient;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.JdkHttpClientBuilder;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpConnectorBuilder#jdk()}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class JdkClientHttpConnectorBuilder extends AbstractClientHttpConnectorBuilder<JdkClientHttpConnector> {

	private final JdkHttpClientBuilder httpClientBuilder;

	JdkClientHttpConnectorBuilder() {
		this(null, new JdkHttpClientBuilder());
	}

	private JdkClientHttpConnectorBuilder(List<Consumer<JdkClientHttpConnector>> customizers,
			JdkHttpClientBuilder httpClientBuilder) {
		super(customizers);
		this.httpClientBuilder = httpClientBuilder;
	}

	@Override
	public JdkClientHttpConnectorBuilder withCustomizer(Consumer<JdkClientHttpConnector> customizer) {
		return new JdkClientHttpConnectorBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
	}

	@Override
	public JdkClientHttpConnectorBuilder withCustomizers(Collection<Consumer<JdkClientHttpConnector>> customizers) {
		return new JdkClientHttpConnectorBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
	}

	/**
	 * Return a new {@link JdkClientHttpConnectorBuilder} that applies additional
	 * customization to the underlying {@link java.net.http.HttpClient.Builder}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link JdkClientHttpConnectorBuilder} instance
	 */
	public JdkClientHttpConnectorBuilder withHttpClientCustomizer(Consumer<HttpClient.Builder> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new JdkClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withCustomizer(httpClientCustomizer));
	}

	@Override
	protected JdkClientHttpConnector createClientHttpConnector(ClientHttpConnectorSettings settings) {
		HttpClient httpClient = this.httpClientBuilder.build(asHttpClientSettings(settings.withReadTimeout(null)));
		JdkClientHttpConnector connector = new JdkClientHttpConnector(httpClient);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::readTimeout).to(connector::setReadTimeout);
		return connector;
	}

	static class Classes {

		static final String HTTP_CLIENT = "java.net.http.HttpClient";

		static boolean present(ClassLoader classLoader) {
			return ClassUtils.isPresent(HTTP_CLIENT, classLoader);
		}

	}

}
