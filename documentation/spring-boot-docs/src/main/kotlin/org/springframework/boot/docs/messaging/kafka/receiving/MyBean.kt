/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.receiving

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Suppress("UNUSED_PARAMETER")
@Component
class MyBean {

	@KafkaListener(topics = ["someTopic"])
	fun processMessage(content: String?) {
		// ...
	}

}

