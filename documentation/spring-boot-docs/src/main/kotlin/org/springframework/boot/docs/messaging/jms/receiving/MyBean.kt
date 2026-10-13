/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.jms.receiving

import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

@Suppress("UNUSED_PARAMETER")
@Component
class MyBean {

	@JmsListener(destination = "someQueue")
	fun processMessage(content: String?) {
		// ...
	}

}

