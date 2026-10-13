/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.logging.structured.otherformats;

import ch.qos.logback.classic.spi.ILoggingEvent;

import org.springframework.boot.logging.structured.StructuredLogFormatter;

class MyCustomFormat implements StructuredLogFormatter<ILoggingEvent> {

	@Override
	public String format(ILoggingEvent event) {
		return "time=" + event.getInstant() + " level=" + event.getLevel() + " message=" + event.getMessage() + "\n";
	}

}
