/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.rabbitmq.sending;

import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final RabbitAmqpAdmin amqpAdmin;

	private final RabbitAmqpTemplate amqpTemplate;

	public MyBean(RabbitAmqpAdmin amqpAdmin, RabbitAmqpTemplate amqpTemplate) {
		this.amqpAdmin = amqpAdmin;
		this.amqpTemplate = amqpTemplate;
	}

	// @fold:on // ...
	public void someOtherMethod() {
		this.amqpTemplate.convertAndSend("hello");
	}
	// @fold:off

}
