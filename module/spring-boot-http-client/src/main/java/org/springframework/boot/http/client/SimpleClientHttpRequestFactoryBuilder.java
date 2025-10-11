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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#simple()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.4.0
 */
public final class SimpleClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<SimpleClientHttpRequestFactory> {

	SimpleClientHttpRequestFactoryBuilder() {
		this(null);
	}

	private SimpleClientHttpRequestFactoryBuilder(
			@Nullable List<Consumer<SimpleClientHttpRequestFactory>> customizers) {
		super(customizers);
	}

	@Override
	public SimpleClientHttpRequestFactoryBuilder withCustomizer(Consumer<SimpleClientHttpRequestFactory> customizer) {
		return new SimpleClientHttpRequestFactoryBuilder(mergedCustomizers(customizer));
	}

	@Override
	public SimpleClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<SimpleClientHttpRequestFactory>> customizers) {
		return new SimpleClientHttpRequestFactoryBuilder(mergedCustomizers(customizers));
	}

	/**
	 * Return a new {@link SimpleClientHttpRequestFactoryBuilder} that applies the given
	 * customizer. This can be useful for applying pre-packaged customizations.
	 * @param customizer the customizer to apply
	 * @return a new {@link SimpleClientHttpRequestFactoryBuilder}
	 * @since 4.0.0
	 */
	public SimpleClientHttpRequestFactoryBuilder with(UnaryOperator<SimpleClientHttpRequestFactoryBuilder> customizer) {
		return customizer.apply(this);
	}

	@Override
	protected SimpleClientHttpRequestFactory createClientHttpRequestFactory(HttpClientSettings settings) {
		SslBundle sslBundle = settings.sslBundle();
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpsRequestFactory(settings);
		Assert.state(sslBundle == null || !sslBundle.getOptions().isSpecified(),
				"SSL Options cannot be specified with Java connections");
		PropertyMapper map = PropertyMapper.get();
		map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
		map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
		return requestFactory;
	}

	/**
	 * {@link SimpleClientHttpsRequestFactory} to configure SSL from an {@link SslBundle}
	 * and {@link Redirects}.
	 */
	private static class SimpleClientHttpsRequestFactory extends SimpleClientHttpRequestFactory {

		private final HttpClientSettings settings;

		SimpleClientHttpsRequestFactory(HttpClientSettings settings) {
			this.settings = settings;
		}

		@Override
		protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
			super.prepareConnection(connection, httpMethod);
			if (this.settings.sslBundle() != null && connection instanceof HttpsURLConnection secureConnection) {
				SSLSocketFactory socketFactory = this.settings.sslBundle().createSslContext().getSocketFactory();
				secureConnection.setSSLSocketFactory(socketFactory);
			}
			if (this.settings.redirects() == HttpRedirects.DONT_FOLLOW) {
				connection.setInstanceFollowRedirects(false);
			}
		}

	}

}
