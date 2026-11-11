/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;

/**
 * Callback interface that can be used to customize a {@link RabbitAmqpTemplate}.
 *
 * @author Eddú Meléndez
 * @since 4.2.0
 */
@FunctionalInterface
public interface RabbitAmqpTemplateCustomizer {

	/**
	 * Callback to customize a {@link RabbitAmqpTemplate} instance.
	 * @param rabbitAmqpTemplate the rabbitAmqpTemplate to customize
	 */
	void customize(RabbitAmqpTemplate rabbitAmqpTemplate);

}
