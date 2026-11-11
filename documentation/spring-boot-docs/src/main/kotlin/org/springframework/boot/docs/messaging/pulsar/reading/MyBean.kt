/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.pulsar.reading

import org.springframework.pulsar.annotation.PulsarReader
import org.springframework.stereotype.Component

@Suppress("UNUSED_PARAMETER")
@Component
class MyBean {

	@PulsarReader(topics = ["someTopic"], startMessageId = "earliest")
	fun processMessage(content: String?) {
		// ...
	}

}

