/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.autoconfigure;

import org.apache.qpid.protonj2.client.ConnectionOptions;

import org.springframework.amqp.client.AmqpConnectionFactory;

/**
 * Callback interface for customizing {@link ConnectionOptions} on the auto-configured
 * {@link AmqpConnectionFactory}.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
@FunctionalInterface
public interface ConnectionOptionsCustomizer {

	/**
	 * Customize the {@link ConnectionOptions}.
	 * @param connectionOptions the connection options to customize
	 */
	void customize(ConnectionOptions connectionOptions);

}
