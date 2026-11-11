/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

import org.springframework.pulsar.config.PulsarContainerFactory;

/**
 * Callback interface that can be implemented by beans wishing to customize a
 * {@link PulsarContainerFactory} to fine-tune its auto-configuration.
 *
 * @param <T> the type of the {@link PulsarContainerFactory}
 * @author Chris Bono
 * @since 4.0.0
 */
@FunctionalInterface
public interface PulsarContainerFactoryCustomizer<T extends PulsarContainerFactory<?, ?>> {

	/**
	 * Customize the container factory.
	 * @param containerFactory the {@code PulsarContainerFactory} to customize
	 */
	void customize(T containerFactory);

}
