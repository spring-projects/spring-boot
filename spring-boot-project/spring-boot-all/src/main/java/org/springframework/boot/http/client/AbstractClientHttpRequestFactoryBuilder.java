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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * Internal base class used for {@link ClientHttpRequestFactoryBuilder} implementations.
 *
 * @param <T> the {@link ClientHttpRequestFactory} type
 * @author Phillip Webb
 */
abstract class AbstractClientHttpRequestFactoryBuilder<T extends ClientHttpRequestFactory>
		implements ClientHttpRequestFactoryBuilder<T> {

	private static final Consumer<?> EMPTY_CUSTOMIZER = (t) -> {
	};

	private final List<Consumer<T>> customizers;

	protected AbstractClientHttpRequestFactoryBuilder(List<Consumer<T>> customizers) {
		this.customizers = (customizers != null) ? customizers : Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	protected static <T> Consumer<T> emptyCustomizer() {
		return (Consumer<T>) EMPTY_CUSTOMIZER;
	}

	protected final List<Consumer<T>> getCustomizers() {
		return this.customizers;
	}

	protected final List<Consumer<T>> mergedCustomizers(Consumer<T> customizer) {
		Assert.notNull(this.customizers, "'customizer' must not be null");
		return merge(this.customizers, List.of(customizer));
	}

	protected final List<Consumer<T>> mergedCustomizers(Collection<Consumer<T>> customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		Assert.noNullElements(customizers, "'customizers' must not contain null elements");
		return merge(this.customizers, customizers);
	}

	private <E> List<E> merge(Collection<E> list, Collection<? extends E> additional) {
		List<E> merged = new ArrayList<>(list);
		merged.addAll(additional);
		return List.copyOf(merged);
	}

	@Override
	@SuppressWarnings("unchecked")
	public final T build(ClientHttpRequestFactorySettings settings) {
		T factory = createClientHttpRequestFactory(
				(settings != null) ? settings : ClientHttpRequestFactorySettings.defaults());
		LambdaSafe.callbacks(Consumer.class, this.customizers, factory).invoke((consumer) -> consumer.accept(factory));
		return factory;
	}

	protected abstract T createClientHttpRequestFactory(ClientHttpRequestFactorySettings settings);

}
