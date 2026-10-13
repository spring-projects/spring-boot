/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Callback interface that can be used to customize a {@link RabbitTemplate}.
 *
 * @author dang zhicairang
 * @since 4.2.0
 */
@FunctionalInterface
public interface RabbitTemplateCustomizer {

	/**
	 * Callback to customize a {@link RabbitTemplate} instance.
	 * @param rabbitTemplate the rabbitTemplate to customize
	 */
	void customize(RabbitTemplate rabbitTemplate);

}
