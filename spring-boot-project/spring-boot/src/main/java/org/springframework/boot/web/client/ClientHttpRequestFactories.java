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

package org.springframework.boot.web.client;

import java.util.function.Supplier;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility class that can be used to create {@link ClientHttpRequestFactory} instances
 * configured using given {@link ClientHttpRequestFactorySettings}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 3.0.0
 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
 * {@link ClientHttpRequestFactoryBuilder}
 */
@Deprecated(since = "3.4.0", forRemoval = true)
public final class ClientHttpRequestFactories {

	private ClientHttpRequestFactories() {
	}

	/**
	 * Return a {@link ClientHttpRequestFactory} implementation with the given
	 * {@code settings} applied. The first of the following implementations whose
	 * dependencies {@link ClassUtils#isPresent are available} is returned:
	 * <ol>
	 * <li>{@link HttpComponentsClientHttpRequestFactory}</li>
	 * <li>{@link JettyClientHttpRequestFactory}</li>
	 * <li>{@link ReactorClientHttpRequestFactory}</li>
	 * <li>{@link SimpleClientHttpRequestFactory}</li>
	 * </ol>
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory}
	 */
	@SuppressWarnings("removal")
	public static ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
		Assert.notNull(settings, "'settings' must not be null");
		return detectBuilder().build(settings.adapt());
	}

	/**
	 * Return a new {@link ClientHttpRequestFactory} of the given
	 * {@code requestFactoryType}, applying {@link ClientHttpRequestFactorySettings} using
	 * reflection if necessary. The following implementations are supported without the
	 * use of reflection:
	 * <ul>
	 * <li>{@link HttpComponentsClientHttpRequestFactory}</li>
	 * <li>{@link JdkClientHttpRequestFactory}</li>
	 * <li>{@link JettyClientHttpRequestFactory}</li>
	 * <li>{@link ReactorClientHttpRequestFactory}</li>
	 * <li>{@link SimpleClientHttpRequestFactory}</li>
	 * </ul>
	 * A {@code requestFactoryType} of {@link ClientHttpRequestFactory} is equivalent to
	 * calling {@link #get(ClientHttpRequestFactorySettings)}.
	 * @param <T> the {@link ClientHttpRequestFactory} type
	 * @param requestFactoryType the {@link ClientHttpRequestFactory} type
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory} instance
	 */
	@SuppressWarnings("removal")
	public static <T extends ClientHttpRequestFactory> T get(Class<T> requestFactoryType,
			ClientHttpRequestFactorySettings settings) {
		Assert.notNull(settings, "'settings' must not be null");
		return getBuilder(requestFactoryType).build(settings.adapt());
	}

	/**
	 * Return a new {@link ClientHttpRequestFactory} from the given supplier, applying
	 * {@link ClientHttpRequestFactorySettings} using reflection.
	 * @param <T> the {@link ClientHttpRequestFactory} type
	 * @param requestFactorySupplier the {@link ClientHttpRequestFactory} supplier
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory} instance
	 */
	@SuppressWarnings("removal")
	public static <T extends ClientHttpRequestFactory> T get(Supplier<T> requestFactorySupplier,
			ClientHttpRequestFactorySettings settings) {
		return ClientHttpRequestFactoryBuilder.of(requestFactorySupplier).build(settings.adapt());
	}

	@SuppressWarnings("unchecked")
	private static <T extends ClientHttpRequestFactory> ClientHttpRequestFactoryBuilder<T> getBuilder(
			Class<T> requestFactoryType) {
		if (requestFactoryType == ClientHttpRequestFactory.class) {
			return (ClientHttpRequestFactoryBuilder<T>) detectBuilder();
		}
		return ClientHttpRequestFactoryBuilder.of(requestFactoryType);
	}

	private static ClientHttpRequestFactoryBuilder<?> detectBuilder() {
		ClientHttpRequestFactoryBuilder<?> builder = ClientHttpRequestFactoryBuilder.detect();
		if (builder instanceof JdkClientHttpRequestFactoryBuilder) {
			// Same logic as earlier versions which did not support JDK client factories
			return ClientHttpRequestFactoryBuilder.simple();
		}
		return builder;
	}

}
