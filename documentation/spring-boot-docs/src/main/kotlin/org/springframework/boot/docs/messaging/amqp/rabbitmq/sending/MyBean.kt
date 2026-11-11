/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.rabbitmq.sending

import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val amqpAdmin: RabbitAmqpAdmin, private val amqpTemplate: RabbitAmqpTemplate) {

	// @fold:on // ...
	fun someOtherMethod() {
		amqpTemplate.convertAndSend("hello")
	}
	// @fold:off

}
