/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.pulsar.sending

import org.apache.pulsar.client.api.PulsarClientException
import org.springframework.pulsar.core.PulsarTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val pulsarTemplate: PulsarTemplate<String>) {

	@Throws(PulsarClientException::class)
	fun someMethod() {
		pulsarTemplate.send("someTopic", "Hello")
	}

}

