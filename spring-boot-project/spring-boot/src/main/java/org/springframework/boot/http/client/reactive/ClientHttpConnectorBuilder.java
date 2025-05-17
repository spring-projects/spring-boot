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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;

/**
 * Interface used to build a fully configured {@link ClientHttpConnector}. Builders for
 * {@link #reactor() Reactor}, {@link #jetty() Jetty}, {@link #httpComponents() Apache
 * HTTP Components} and {@link #jdk() JDK} can be obtained using the factory methods on
 * this interface. The {@link #of(Class)} method may be used to instantiate based on the
 * connector type.
 *
 * @param <T> the {@link ClientHttpConnector} type
 * @author Phillip Webb
 * @since 3.5.0
 */
@FunctionalInterface
public interface ClientHttpConnectorBuilder<T extends ClientHttpConnector> {

	/**
	 * Build a default configured {@link ClientHttpConnectorBuilder}.
	 * @return a default configured {@link ClientHttpConnectorBuilder}.
	 */
	default T build() {
		return build(null);
	}

	/**
	 * Build a fully configured {@link ClientHttpConnector}, applying the given
	 * {@code settings} if they are provided.
	 * @param settings the settings to apply or {@code null}
	 * @return a fully configured {@link ClientHttpConnector}.
	 */
	T build(ClientHttpConnectorSettings settings);

	/**
	 * Return a new {@link ClientHttpConnectorBuilder} that applies the given customizer
	 * to the {@link ClientHttpConnector} after it has been built.
	 * @param customizer the customizers to apply
	 * @return a new {@link ClientHttpConnectorBuilder} instance
	 */
	default ClientHttpConnectorBuilder<T> withCustomizer(Consumer<T> customizer) {
		return withCustomizers(List.of(customizer));
	}

	/**
	 * Return a new {@link ClientHttpConnectorBuilder} that applies the given customizers
	 * to the {@link ClientHttpConnector} after it has been built.
	 * @param customizers the customizers to apply
	 * @return a new {@link ClientHttpConnectorBuilder} instance
	 */
	@SuppressWarnings("unchecked")
	default ClientHttpConnectorBuilder<T> withCustomizers(Collection<Consumer<T>> customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		Assert.noNullElements(customizers, "'customizers' must not contain null elements");
		return (settings) -> {
			T factory = build(settings);
			LambdaSafe.callbacks(Consumer.class, customizers, factory).invoke((consumer) -> consumer.accept(factory));
			return factory;
		};
	}

	/**
	 * Return a {@link HttpComponentsClientHttpConnectorBuilder} that can be used to build
	 * a {@link HttpComponentsClientHttpConnector}.
	 * @return a new {@link HttpComponentsClientHttpConnectorBuilder}
	 */
	static HttpComponentsClientHttpConnectorBuilder httpComponents() {
		return new HttpComponentsClientHttpConnectorBuilder();
	}

	/**
	 * Return a {@link JettyClientHttpConnectorBuilder} that can be used to build a
	 * {@link JettyClientHttpConnector}.
	 * @return a new {@link JettyClientHttpConnectorBuilder}
	 */
	static JettyClientHttpConnectorBuilder jetty() {
		return new JettyClientHttpConnectorBuilder();
	}

	/**
	 * Return a {@link ReactorClientHttpConnectorBuilder} that can be used to build a
	 * {@link ReactorClientHttpConnector}.
	 * @return a new {@link ReactorClientHttpConnectorBuilder}
	 */
	static ReactorClientHttpConnectorBuilder reactor() {
		return new ReactorClientHttpConnectorBuilder();
	}

	/**
	 * Return a {@link JdkClientHttpConnectorBuilder} that can be used to build a
	 * {@link JdkClientHttpConnector} .
	 * @return a new {@link JdkClientHttpConnectorBuilder}
	 */
	static JdkClientHttpConnectorBuilder jdk() {
		return new JdkClientHttpConnectorBuilder();
	}

	/**
	 * Return a new {@link ClientHttpConnectorBuilder} for the given
	 * {@code requestFactoryType}. The following implementations are supported:
	 * <ul>
	 * <li>{@link ReactorClientHttpConnector}</li>
	 * <li>{@link JettyClientHttpConnector}</li>
	 * <li>{@link HttpComponentsClientHttpConnector}</li>
	 * <li>{@link JdkClientHttpConnector}</li>
	 * </ul>
	 * @param <T> the {@link ClientHttpConnector} type
	 * @param clientHttpConnectorType the {@link ClientHttpConnector} type
	 * @return a new {@link ClientHttpConnectorBuilder}
	 */
	@SuppressWarnings("unchecked")
	static <T extends ClientHttpConnector> ClientHttpConnectorBuilder<T> of(Class<T> clientHttpConnectorType) {
		Assert.notNull(clientHttpConnectorType, "'requestFactoryType' must not be null");
		Assert.isTrue(clientHttpConnectorType != ClientHttpConnector.class,
				"'clientHttpConnectorType' must be an implementation of ClientHttpConnector");
		if (clientHttpConnectorType == ReactorClientHttpConnector.class) {
			return (ClientHttpConnectorBuilder<T>) reactor();
		}
		if (clientHttpConnectorType == JettyClientHttpConnector.class) {
			return (ClientHttpConnectorBuilder<T>) jetty();
		}
		if (clientHttpConnectorType == HttpComponentsClientHttpConnector.class) {
			return (ClientHttpConnectorBuilder<T>) httpComponents();
		}
		if (clientHttpConnectorType == JdkClientHttpConnector.class) {
			return (ClientHttpConnectorBuilder<T>) jdk();
		}
		throw new IllegalArgumentException(
				"'clientHttpConnectorType' %s is not supported".formatted(clientHttpConnectorType.getName()));
	}

	/**
	 * Detect the most suitable {@link ClientHttpConnectorBuilder} based on the classpath.
	 * The method favors builders in the following order:
	 * <ol>
	 * <li>{@link #reactor()}</li>
	 * <li>{@link #jetty()}</li>
	 * <li>{@link #httpComponents()}</li>
	 * <li>{@link #jdk()}</li>
	 * </ol>
	 * @return the most suitable {@link ClientHttpConnectorBuilder} for the classpath
	 */
	static ClientHttpConnectorBuilder<? extends ClientHttpConnector> detect() {
		return detect(null);
	}

	/**
	 * Detect the most suitable {@link ClientHttpConnectorBuilder} based on the classpath.
	 * The method favors builders in the following order:
	 * <ol>
	 * <li>{@link #reactor()}</li>
	 * <li>{@link #jetty()}</li>
	 * <li>{@link #httpComponents()}</li>
	 * <li>{@link #jdk()}</li>
	 * </ol>
	 * @param classLoader the class loader to use for detection
	 * @return the most suitable {@link ClientHttpConnectorBuilder} for the classpath
	 */
	static ClientHttpConnectorBuilder<? extends ClientHttpConnector> detect(ClassLoader classLoader) {
		if (ReactorClientHttpConnectorBuilder.Classes.present(classLoader)) {
			return reactor();
		}
		if (JettyClientHttpConnectorBuilder.Classes.present(classLoader)) {
			return jetty();
		}
		if (HttpComponentsClientHttpConnectorBuilder.Classes.present(classLoader)) {
			return httpComponents();
		}
		if (JdkClientHttpConnectorBuilder.Classes.present(classLoader)) {
			return jdk();
		}
		throw new IllegalStateException("Unable to detect any ClientHttpConnectorBuilder");
	}

}
