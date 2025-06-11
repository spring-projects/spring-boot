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

package org.springframework.boot.http.client.autoconfigure.reactive;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.reactive.AbstractClientHttpConnectorProperties.Connector;
import org.springframework.boot.http.client.autoconfigure.reactive.AbstractClientHttpConnectorProperties.Ssl;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.StringUtils;

/**
 * Helper class to create {@link ClientHttpConnectorBuilder} and
 * {@link ClientHttpConnectorSettings}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class ClientHttpConnectors {

	private final ObjectFactory<SslBundles> sslBundles;

	private final AbstractClientHttpConnectorProperties[] orderedProperties;

	public ClientHttpConnectors(ObjectFactory<SslBundles> sslBundles,
			AbstractClientHttpConnectorProperties... orderedProperties) {
		this.sslBundles = sslBundles;
		this.orderedProperties = orderedProperties;
	}

	public ClientHttpConnectorBuilder<?> builder(ClassLoader classLoader) {
		Connector connector = getProperty(AbstractClientHttpConnectorProperties::getConnector);
		return (connector != null) ? connector.builder() : ClientHttpConnectorBuilder.detect(classLoader);
	}

	public ClientHttpConnectorSettings settings() {
		HttpRedirects redirects = getProperty(AbstractClientHttpConnectorProperties::getRedirects);
		Duration connectTimeout = getProperty(AbstractClientHttpConnectorProperties::getConnectTimeout);
		Duration readTimeout = getProperty(AbstractClientHttpConnectorProperties::getReadTimeout);
		String sslBundleName = getProperty(AbstractClientHttpConnectorProperties::getSsl, Ssl::getBundle,
				StringUtils::hasText);
		SslBundle sslBundle = (StringUtils.hasLength(sslBundleName))
				? this.sslBundles.getObject().getBundle(sslBundleName) : null;
		return new ClientHttpConnectorSettings(redirects, connectTimeout, readTimeout, sslBundle);
	}

	private <T> T getProperty(Function<AbstractClientHttpConnectorProperties, T> accessor) {
		return getProperty(accessor, Function.identity(), Objects::nonNull);
	}

	private <P, T> T getProperty(Function<AbstractClientHttpConnectorProperties, P> accessor, Function<P, T> extractor,
			Predicate<T> predicate) {
		for (AbstractClientHttpConnectorProperties properties : this.orderedProperties) {
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
