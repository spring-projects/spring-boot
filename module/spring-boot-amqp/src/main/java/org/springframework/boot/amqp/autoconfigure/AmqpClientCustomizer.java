/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.autoconfigure;

import org.springframework.amqp.client.AmqpClient;

/**
 * Callback interface that can be used to customize the auto-configured
 * {@link AmqpClient.Builder}.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
@FunctionalInterface
public interface AmqpClientCustomizer {

	/**
	 * Callback to customize a {@link AmqpClient.Builder} instance.
	 * @param amqpClientBuilder the client builder to customize
	 */
	void customize(AmqpClient.Builder amqpClientBuilder);

}
