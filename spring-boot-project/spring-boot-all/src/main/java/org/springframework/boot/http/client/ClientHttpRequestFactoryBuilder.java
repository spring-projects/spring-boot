/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * Interface used to build a fully configured {@link ClientHttpRequestFactory}. Builders
 * for {@link #httpComponents() Apache HTTP Components}, {@link #jetty() Jetty},
 * {@link #reactor() Reactor}, {@link #jdk() JDK} and {@link #simple() simple client} can
 * be obtained using the factory methods on this interface. The {@link #of(Class)} and
 * {@link #of(Supplier)} methods may be used to instantiate other
 * {@link ClientHttpRequestFactory} instances using reflection.
 *
 * @param <T> the {@link ClientHttpRequestFactory} type
 * @author Phillip Webb
 * @since 3.4.0
 */
@FunctionalInterface
public interface ClientHttpRequestFactoryBuilder<T extends ClientHttpRequestFactory> {

	/**
	 * Build a default configured {@link ClientHttpRequestFactory}.
	 * @return a default configured {@link ClientHttpRequestFactory}.
	 */
	default T build() {
		return build(null);
	}

	/**
	 * Build a fully configured {@link ClientHttpRequestFactory}, applying the given
	 * {@code settings} if they are provided.
	 * @param settings the settings to apply or {@code null}
	 * @return a fully configured {@link ClientHttpRequestFactory}.
	 */
	T build(ClientHttpRequestFactorySettings settings);

	/**
	 * Return a new {@link ClientHttpRequestFactoryBuilder} that applies the given
	 * customizer to the {@link ClientHttpRequestFactory} after it has been built.
	 * @param customizer the customizers to apply
	 * @return a new {@link ClientHttpRequestFactoryBuilder} instance
	 */
	default ClientHttpRequestFactoryBuilder<T> withCustomizer(Consumer<T> customizer) {
		return withCustomizers(List.of(customizer));
	}

	/**
	 * Return a new {@link ClientHttpRequestFactoryBuilder} that applies the given
	 * customizers to the {@link ClientHttpRequestFactory} after it has been built.
	 * @param customizers the customizers to apply
	 * @return a new {@link ClientHttpRequestFactoryBuilder} instance
	 */
	@SuppressWarnings("unchecked")
	default ClientHttpRequestFactoryBuilder<T> withCustomizers(Collection<Consumer<T>> customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		Assert.noNullElements(customizers, "'customizers' must not contain null elements");
		return (settings) -> {
			T factory = build(settings);
			LambdaSafe.callbacks(Consumer.class, customizers, factory).invoke((consumer) -> consumer.accept(factory));
			return factory;
		};
	}

	/**
	 * Return a {@link HttpComponentsClientHttpRequestFactoryBuilder} that can be used to
	 * build a {@link HttpComponentsClientHttpRequestFactory}.
	 * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder}
	 */
	static HttpComponentsClientHttpRequestFactoryBuilder httpComponents() {
		return new HttpComponentsClientHttpRequestFactoryBuilder();
	}

	/**
	 * Return a {@link JettyClientHttpRequestFactoryBuilder} that can be used to build a
	 * {@link JettyClientHttpRequestFactory}.
	 * @return a new {@link JettyClientHttpRequestFactoryBuilder}
	 */
	static JettyClientHttpRequestFactoryBuilder jetty() {
		return new JettyClientHttpRequestFactoryBuilder();
	}

	/**
	 * Return a {@link ReactorClientHttpRequestFactoryBuilder} that can be used to build a
	 * {@link ReactorClientHttpRequestFactory}.
	 * @return a new {@link ReactorClientHttpRequestFactoryBuilder}
	 */
	static ReactorClientHttpRequestFactoryBuilder reactor() {
		return new ReactorClientHttpRequestFactoryBuilder();
	}

	/**
	 * Return a {@link JdkClientHttpRequestFactoryBuilder} that can be used to build a
	 * {@link JdkClientHttpRequestFactory} .
	 * @return a new {@link JdkClientHttpRequestFactoryBuilder}
	 */
	static JdkClientHttpRequestFactoryBuilder jdk() {
		return new JdkClientHttpRequestFactoryBuilder();
	}

	/**
	 * Return a {@link SimpleClientHttpRequestFactoryBuilder} that can be used to build a
	 * {@link SimpleClientHttpRequestFactory} .
	 * @return a new {@link SimpleClientHttpRequestFactoryBuilder}
	 */
	static SimpleClientHttpRequestFactoryBuilder simple() {
		return new SimpleClientHttpRequestFactoryBuilder();
	}

	/**
	 * Return a new {@link ClientHttpRequestFactoryBuilder} for the given
	 * {@code requestFactoryType}. The following implementations are supported without the
	 * use of reflection:
	 * <ul>
	 * <li>{@link HttpComponentsClientHttpRequestFactory}</li>
	 * <li>{@link JdkClientHttpRequestFactory}</li>
	 * <li>{@link JettyClientHttpRequestFactory}</li>
	 * <li>{@link ReactorClientHttpRequestFactory}</li>
	 * <li>{@link SimpleClientHttpRequestFactory}</li>
	 * </ul>
	 * @param <T> the {@link ClientHttpRequestFactory} type
	 * @param requestFactoryType the {@link ClientHttpRequestFactory} type
	 * @return a new {@link ClientHttpRequestFactoryBuilder}
	 */
	@SuppressWarnings("unchecked")
	static <T extends ClientHttpRequestFactory> ClientHttpRequestFactoryBuilder<T> of(Class<T> requestFactoryType) {
		Assert.notNull(requestFactoryType, "'requestFactoryType' must not be null");
		Assert.isTrue(requestFactoryType != ClientHttpRequestFactory.class,
				"'requestFactoryType' must be an implementation of ClientHttpRequestFactory");
		if (requestFactoryType == HttpComponentsClientHttpRequestFactory.class) {
			return (ClientHttpRequestFactoryBuilder<T>) httpComponents();
		}
		if (requestFactoryType == JettyClientHttpRequestFactory.class) {
			return (ClientHttpRequestFactoryBuilder<T>) jetty();
		}
		if (requestFactoryType == ReactorClientHttpRequestFactory.class) {
			return (ClientHttpRequestFactoryBuilder<T>) reactor();
		}
		if (requestFactoryType == JdkClientHttpRequestFactory.class) {
			return (ClientHttpRequestFactoryBuilder<T>) jdk();
		}
		if (requestFactoryType == SimpleClientHttpRequestFactory.class) {
			return (ClientHttpRequestFactoryBuilder<T>) simple();
		}
		return new ReflectiveComponentsClientHttpRequestFactoryBuilder<>(requestFactoryType);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactoryBuilder} from the given supplier, using
	 * reflection to ultimately apply the {@link ClientHttpRequestFactorySettings}.
	 * @param <T> the {@link ClientHttpRequestFactory} type
	 * @param requestFactorySupplier the {@link ClientHttpRequestFactory} supplier
	 * @return a new {@link ClientHttpRequestFactoryBuilder}
	 */
	static <T extends ClientHttpRequestFactory> ClientHttpRequestFactoryBuilder<T> of(
			Supplier<T> requestFactorySupplier) {
		return new ReflectiveComponentsClientHttpRequestFactoryBuilder<>(requestFactorySupplier);
	}

	/**
	 * Detect the most suitable {@link ClientHttpRequestFactoryBuilder} based on the
	 * classpath. The methods favors builders in the following order:
	 * <ol>
	 * <li>{@link #httpComponents()}</li>
	 * <li>{@link #jetty()}</li>
	 * <li>{@link #reactor()}</li>
	 * <li>{@link #jdk()}</li>
	 * <li>{@link #simple()}</li>
	 * </ol>
	 * @return the most suitable {@link ClientHttpRequestFactoryBuilder} for the classpath
	 */
	static ClientHttpRequestFactoryBuilder<? extends ClientHttpRequestFactory> detect() {
		if (HttpComponentsClientHttpRequestFactoryBuilder.Classes.PRESENT) {
			return httpComponents();
		}
		if (JettyClientHttpRequestFactoryBuilder.Classes.PRESENT) {
			return jetty();
		}
		if (ReactorClientHttpRequestFactoryBuilder.Classes.PRESENT) {
			return reactor();
		}
		if (JdkClientHttpRequestFactoryBuilder.Classes.PRESENT) {
			return jdk();
		}
		return simple();
	}

}
