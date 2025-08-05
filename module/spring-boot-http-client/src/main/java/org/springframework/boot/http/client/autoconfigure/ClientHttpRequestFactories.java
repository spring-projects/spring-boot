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

package org.springframework.boot.http.client.autoconfigure;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.AbstractHttpRequestFactoryProperties.Factory;
import org.springframework.boot.http.client.autoconfigure.AbstractHttpRequestFactoryProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.StringUtils;

/**
 * Helper class to create {@link ClientHttpRequestFactoryBuilder} and
 * {@link ClientHttpRequestFactorySettings}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class ClientHttpRequestFactories {

	private final ObjectFactory<SslBundles> sslBundles;

	private final @Nullable AbstractHttpRequestFactoryProperties[] orderedProperties;

	public ClientHttpRequestFactories(ObjectFactory<SslBundles> sslBundles,
			@Nullable AbstractHttpRequestFactoryProperties... orderedProperties) {
		this.sslBundles = sslBundles;
		this.orderedProperties = orderedProperties;
	}

	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public ClientHttpRequestFactoryBuilder<?> builder(@Nullable ClassLoader classLoader) {
		Factory factory = getProperty(AbstractHttpRequestFactoryProperties::getFactory);
		return (factory != null) ? factory.builder() : ClientHttpRequestFactoryBuilder.detect(classLoader);
	}

	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public ClientHttpRequestFactorySettings settings() {
		HttpRedirects redirects = getProperty(AbstractHttpRequestFactoryProperties::getRedirects);
		Duration connectTimeout = getProperty(AbstractHttpRequestFactoryProperties::getConnectTimeout);
		Duration readTimeout = getProperty(AbstractHttpRequestFactoryProperties::getReadTimeout);
		String sslBundleName = getProperty(AbstractHttpRequestFactoryProperties::getSsl, Ssl::getBundle,
				StringUtils::hasLength);
		SslBundle sslBundle = (StringUtils.hasLength(sslBundleName))
				? this.sslBundles.getObject().getBundle(sslBundleName) : null;
		return new ClientHttpRequestFactorySettings(redirects, connectTimeout, readTimeout, sslBundle);
	}

	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	private <T> @Nullable T getProperty(Function<AbstractHttpRequestFactoryProperties, @Nullable T> accessor) {
		return getProperty(accessor, Function.identity(), Objects::nonNull);
	}

	private <P, T> @Nullable T getProperty(Function<AbstractHttpRequestFactoryProperties, @Nullable P> accessor,
			Function<P, T> extractor, Predicate<@Nullable T> predicate) {
		for (AbstractHttpRequestFactoryProperties properties : this.orderedProperties) {
			if (properties != null) {
				P value = accessor.apply(properties);
				T extracted = (value != null) ? extractor.apply(value) : null;
				if (predicate.test(extracted)) {
					return extracted;
				}
			}
		}
		return null;
	}

}
