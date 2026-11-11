/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

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

	private final List<Consumer<T>> customizers;

	protected AbstractClientHttpRequestFactoryBuilder(@Nullable List<Consumer<T>> customizers) {
		this.customizers = (customizers != null) ? customizers : Collections.emptyList();
	}

	protected final List<Consumer<T>> getCustomizers() {
		return this.customizers;
	}

	protected final List<Consumer<T>> mergedCustomizers(Consumer<T> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
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
	public final T build(@Nullable HttpClientSettings settings) {
		T factory = createClientHttpRequestFactory((settings != null) ? settings : HttpClientSettings.defaults());
		LambdaSafe.callbacks(Consumer.class, this.customizers, factory).invoke((consumer) -> consumer.accept(factory));
		return factory;
	}

	protected abstract T createClientHttpRequestFactory(HttpClientSettings settings);

}
