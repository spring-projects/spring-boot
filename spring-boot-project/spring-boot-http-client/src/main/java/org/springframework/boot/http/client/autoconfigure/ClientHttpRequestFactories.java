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

	private final AbstractHttpRequestFactoryProperties[] orderedProperties;

	public ClientHttpRequestFactories(ObjectFactory<SslBundles> sslBundles,
			AbstractHttpRequestFactoryProperties... orderedProperties) {
		this.sslBundles = sslBundles;
		this.orderedProperties = orderedProperties;
	}

	public ClientHttpRequestFactoryBuilder<?> builder(ClassLoader classLoader) {
		Factory factory = getProperty(AbstractHttpRequestFactoryProperties::getFactory);
		return (factory != null) ? factory.builder() : ClientHttpRequestFactoryBuilder.detect(classLoader);
	}

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

	private <T> T getProperty(Function<AbstractHttpRequestFactoryProperties, T> accessor) {
		return getProperty(accessor, Function.identity(), Objects::nonNull);
	}

	private <P, T> T getProperty(Function<AbstractHttpRequestFactoryProperties, P> accessor, Function<P, T> extractor,
			Predicate<T> predicate) {
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
