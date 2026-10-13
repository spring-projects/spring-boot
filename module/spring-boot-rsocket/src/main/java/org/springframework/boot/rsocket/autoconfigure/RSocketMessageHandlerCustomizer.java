/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.autoconfigure;

import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

/**
 * Callback interface that can be used to customize a {@link RSocketMessageHandler}.
 *
 * @author Aarti Gupta
 * @author Madhura Bhave
 * @since 4.0.0
 */
@FunctionalInterface
public interface RSocketMessageHandlerCustomizer {

	/**
	 * Customize the {@link RSocketMessageHandler}.
	 * @param messageHandler the message handler to customize
	 */
	void customize(RSocketMessageHandler messageHandler);

}
