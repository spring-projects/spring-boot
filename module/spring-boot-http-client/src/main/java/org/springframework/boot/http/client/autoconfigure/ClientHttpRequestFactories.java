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

	private final @Nullable ClientHttpRequestFactoryBuilder<?> fallbackBuilder;

	private final @Nullable ClientHttpRequestFactorySettings fallbackSettings;

	public ClientHttpRequestFactories(ObjectFactory<SslBundles> sslBundles,
			@Nullable AbstractHttpRequestFactoryProperties... orderedProperties) {
		this(null, null, sslBundles, orderedProperties);
	}

	public ClientHttpRequestFactories(@Nullable ClientHttpRequestFactoryBuilder<?> fallbackBuilder,
			@Nullable ClientHttpRequestFactorySettings fallbackSettings, ObjectFactory<SslBundles> sslBundles,
			@Nullable AbstractHttpRequestFactoryProperties... orderedProperties) {
		this.fallbackBuilder = fallbackBuilder;
		this.fallbackSettings = fallbackSettings;
		this.sslBundles = sslBundles;
		this.orderedProperties = orderedProperties;
	}

	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public ClientHttpRequestFactoryBuilder<?> builder(@Nullable ClassLoader classLoader) {
		Factory factory = getProperty(AbstractHttpRequestFactoryProperties::getFactory, Objects::nonNull, null,
				Function.identity());
		if (factory != null) {
			return factory.builder();
		}
		return (this.fallbackBuilder != null) ? this.fallbackBuilder
				: ClientHttpRequestFactoryBuilder.detect(classLoader);
	}

	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	public ClientHttpRequestFactorySettings settings() {
		HttpRedirects redirects = getProperty(AbstractHttpRequestFactoryProperties::getRedirects, Objects::nonNull,
				this.fallbackSettings, ClientHttpRequestFactorySettings::redirects);
		Duration connectTimeout = getProperty(AbstractHttpRequestFactoryProperties::getConnectTimeout, Objects::nonNull,
				this.fallbackSettings, ClientHttpRequestFactorySettings::connectTimeout);
		Duration readTimeout = getProperty(AbstractHttpRequestFactoryProperties::getReadTimeout, Objects::nonNull,
				this.fallbackSettings, ClientHttpRequestFactorySettings::readTimeout);
		String sslBundleName = getProperty(AbstractHttpRequestFactoryProperties::getSsl, Ssl::getBundle,
				StringUtils::hasLength, null, Function.identity());
		SslBundle sslBundle = (StringUtils.hasLength(sslBundleName))
				? this.sslBundles.getObject().getBundle(sslBundleName) : fallbackSslBundle();
		return new ClientHttpRequestFactorySettings(redirects, connectTimeout, readTimeout, sslBundle);
	}

	private @Nullable SslBundle fallbackSslBundle() {
		return (this.fallbackSettings != null) ? this.fallbackSettings.sslBundle() : null;
	}

	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	private <T, F> @Nullable T getProperty(Function<AbstractHttpRequestFactoryProperties, @Nullable T> accessor,
			Predicate<@Nullable T> predicate, @Nullable F fallback, Function<F, @Nullable T> fallbackAccessor) {
		return getProperty(accessor, Function.identity(), predicate, fallback, fallbackAccessor);
	}

	@SuppressWarnings("NullAway") // Lambda isn't detected with the correct nullability
	private <P, T, F> @Nullable T getProperty(Function<AbstractHttpRequestFactoryProperties, @Nullable P> accessor,
			Function<P, @Nullable T> extractor, Predicate<@Nullable T> predicate, @Nullable F fallback,
			Function<F, @Nullable T> fallbackAccessor) {
		for (AbstractHttpRequestFactoryProperties properties : this.orderedProperties) {
			if (properties != null) {
				P value = accessor.apply(properties);
				T extracted = (value != null) ? extractor.apply(value) : null;
				if (predicate.test(extracted)) {
					return extracted;
				}
			}
		}
		return (fallback != null) ? fallbackAccessor.apply(fallback) : null;
	}

}
