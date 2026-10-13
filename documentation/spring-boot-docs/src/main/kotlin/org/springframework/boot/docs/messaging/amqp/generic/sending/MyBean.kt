/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.generic.sending

import org.springframework.amqp.client.AmqpClient
import org.springframework.stereotype.Component

@Component
class MyBean(private val amqpClient: AmqpClient) {

	// @fold:on // ...
	fun someOtherMethod(msg: String) {
		amqpClient.to("/queues/test").body(msg).send()
	}
	// @fold:off

}

