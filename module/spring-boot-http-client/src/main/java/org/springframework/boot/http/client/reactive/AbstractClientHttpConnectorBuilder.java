/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.reactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.util.Assert;

/**
 * Internal base class used for {@link ClientHttpConnectorBuilder} implementations.
 *
 * @param <T> the {@link ClientHttpConnector} type
 * @author Phillip Webb
 */
abstract class AbstractClientHttpConnectorBuilder<T extends ClientHttpConnector>
		implements ClientHttpConnectorBuilder<T> {

	private final List<Consumer<T>> customizers;

	protected AbstractClientHttpConnectorBuilder(@Nullable List<Consumer<T>> customizers) {
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
		T connector = createClientHttpConnector((settings != null) ? settings : HttpClientSettings.defaults());
		LambdaSafe.callbacks(Consumer.class, this.customizers, connector)
			.invoke((consumer) -> consumer.accept(connector));
		return connector;
	}

	protected abstract T createClientHttpConnector(HttpClientSettings settings);

}
