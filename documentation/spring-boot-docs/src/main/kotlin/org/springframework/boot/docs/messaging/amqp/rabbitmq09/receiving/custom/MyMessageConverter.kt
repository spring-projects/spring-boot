/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.rabbitmq09.receiving.custom

import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.support.converter.MessageConverter

internal class MyMessageConverter : MessageConverter {

	override fun toMessage(`object`: Any, messageProperties: MessageProperties): Message {
		return Message(byteArrayOf())
	}

	override fun fromMessage(message: Message): Any {
		return Any()
	}

}

