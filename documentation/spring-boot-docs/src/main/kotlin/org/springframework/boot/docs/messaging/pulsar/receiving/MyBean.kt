/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.pulsar.receiving

import org.springframework.pulsar.annotation.PulsarListener
import org.springframework.stereotype.Component

@Suppress("UNUSED_PARAMETER")
@Component
class MyBean {

	@PulsarListener(topics = ["someTopic"])
	fun processMessage(content: String?) {
		// ...
	}

}

