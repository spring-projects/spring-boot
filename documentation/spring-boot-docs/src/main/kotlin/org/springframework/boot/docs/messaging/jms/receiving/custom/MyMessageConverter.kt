/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.jms.receiving.custom

import jakarta.jms.Message
import jakarta.jms.Session
import org.springframework.jms.support.converter.MessageConverter

internal class MyMessageConverter : MessageConverter {

	override fun toMessage(`object`: Any, session: Session): Message {
		return session.createObjectMessage()
	}

	override fun fromMessage(message: Message): Any {
		return Any()
	}

}

