/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.pulsar.config.PulsarContainerFactory;
import org.springframework.pulsar.core.PulsarConsumerFactory;

/**
 * Invokes the available {@link PulsarContainerFactoryCustomizer} instances in the context
 * for a given {@link PulsarConsumerFactory}.
 *
 * @author Chris Bono
 */
class PulsarContainerFactoryCustomizers {

	private final List<PulsarContainerFactoryCustomizer<?>> customizers;

	PulsarContainerFactoryCustomizers(@Nullable List<? extends PulsarContainerFactoryCustomizer<?>> customizers) {
		this.customizers = (customizers != null) ? new ArrayList<>(customizers) : Collections.emptyList();
	}

	/**
	 * Customize the specified {@link PulsarContainerFactory}. Locates all
	 * {@link PulsarContainerFactoryCustomizer} beans able to handle the specified
	 * instance and invoke {@link PulsarContainerFactoryCustomizer#customize} on them.
	 * @param <T> the type of container factory
	 * @param containerFactory the container factory to customize
	 * @return the customized container factory
	 */
	@SuppressWarnings("unchecked")
	<T extends PulsarContainerFactory<?, ?>> T customize(T containerFactory) {
		LambdaSafe.callbacks(PulsarContainerFactoryCustomizer.class, this.customizers, containerFactory)
			.withLogger(PulsarContainerFactoryCustomizers.class)
			.invoke((customizer) -> customizer.customize(containerFactory));
		return containerFactory;
	}

}
