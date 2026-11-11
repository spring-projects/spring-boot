/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.messaging;

import org.springframework.messaging.rsocket.RSocketStrategies;

/**
 * Callback interface that can be used to customize codecs configuration for an RSocket
 * client and/or server with {@link RSocketStrategies}.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@FunctionalInterface
public interface RSocketStrategiesCustomizer {

	/**
	 * Callback to customize a {@link RSocketStrategies#builder()} instance.
	 * @param strategies rSocket codec strategies to customize
	 */
	void customize(RSocketStrategies.Builder strategies);

}
