/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.rabbitmq.receiving

import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Suppress("UNUSED_PARAMETER")
@Component
@RabbitListener(queues = ["someQueue"])
class MyBean {

	@RabbitHandler
	fun processMessage(content: String?) {
		// ...
	}

}
