/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.logging.structured.otherformats

import ch.qos.logback.classic.spi.ILoggingEvent
import org.springframework.boot.logging.structured.StructuredLogFormatter

class MyCustomFormat : StructuredLogFormatter<ILoggingEvent> {

	override fun format(event: ILoggingEvent): String {
		return "time=${event.instant} level=${event.level} message=${event.message}\n"
	}

}
