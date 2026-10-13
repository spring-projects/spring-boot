/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.jms.sending

import org.springframework.jms.core.JmsClient
import org.springframework.stereotype.Component

@Component
class MyBean(private val jmsClient: JmsClient) {

	// @fold:on // ...
	fun someMethod() {
		jmsClient.destination("myQueue").send("hello")
	}
	// @fold:off

}

