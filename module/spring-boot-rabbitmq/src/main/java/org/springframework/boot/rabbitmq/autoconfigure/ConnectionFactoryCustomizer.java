/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import com.rabbitmq.client.ConnectionFactory;

/**
 * Callback interface that can be implemented by beans wishing to customize the RabbitMQ
 * {@link ConnectionFactory} to fine-tune its auto-configuration.
 *
 * @author Andy Wilkinson
 * @since 4.2.0
 */
@FunctionalInterface
public interface ConnectionFactoryCustomizer {

	/**
	 * Customize the {@link ConnectionFactory}.
	 * @param factory the factory to customize
	 */
	void customize(ConnectionFactory factory);

}
