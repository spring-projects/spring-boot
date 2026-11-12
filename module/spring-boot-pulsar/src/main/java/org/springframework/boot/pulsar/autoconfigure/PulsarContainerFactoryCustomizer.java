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
 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
 * <a href="https://github.com/apache/pulsar-spring">pulsar-spring</a>
 */
@FunctionalInterface
@Deprecated(since = "4.2.0", forRemoval = true)
public interface PulsarContainerFactoryCustomizer<T extends PulsarContainerFactory<?, ?>> {

	/**
	 * Customize the container factory.
	 * @param containerFactory the {@code PulsarContainerFactory} to customize
	 */
	void customize(T containerFactory);

}
