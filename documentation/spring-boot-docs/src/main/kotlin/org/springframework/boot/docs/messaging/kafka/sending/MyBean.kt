/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.sending

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val kafkaTemplate: KafkaTemplate<String, String>) {

	// @fold:on // ...
	fun someMethod() {
		kafkaTemplate.send("someTopic", "Hello")
	}
	// @fold:off

}

