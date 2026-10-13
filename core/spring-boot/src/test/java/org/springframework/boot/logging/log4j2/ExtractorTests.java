/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Extractor}.
 *
 * @author Phillip Webb
 */
class ExtractorTests {

	@Test
	void messageAndStackTraceWhenNoPrinterPrintsUsingLoggingSystem() {
		Extractor extractor = new Extractor(null);
		assertThat(extractor.messageAndStackTrace(createEvent())).startsWith("TestMessage\n\n")
			.contains("java.lang.RuntimeException: Boom!");
	}

	@Test
	void messageAndStackTraceWhenPrinterPrintsUsingPrinter() {
		Extractor extractor = new Extractor(new SimpleStackTracePrinter());
		assertThat(extractor.messageAndStackTrace(createEvent()))
			.isEqualTo("TestMessage\n\nstacktrace:RuntimeException");
	}

	@Test
	void stackTraceWhenNoPrinterPrintsUsingLoggingSystem() {
		Extractor extractor = new Extractor(null);
		assertThat(extractor.stackTrace(createEvent())).contains("java.lang.RuntimeException: Boom!");
	}

	@Test
	void stackTraceWhenPrinterPrintsUsingPrinter() {
		Extractor extractor = new Extractor(new SimpleStackTracePrinter());
		assertThat(extractor.stackTrace(createEvent())).isEqualTo("stacktrace:RuntimeException");
	}

	private LogEvent createEvent() {
		MutableLogEvent event = new MutableLogEvent();
		event.setMessage(new SimpleMessage("TestMessage"));
		event.setThrown(new RuntimeException("Boom!"));
		return event;
	}

}
