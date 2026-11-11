/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
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
		Extractor extractor = new Extractor(null, createConverter());
		assertThat(extractor.messageAndStackTrace(createEvent())).startsWith("TestMessage\n\n")
			.contains("java.lang.RuntimeException: Boom!");
	}

	@Test
	void messageAndStackTraceWhenNoPrinterPrintsUsingPrinter() {
		Extractor extractor = new Extractor(new SimpleStackTracePrinter(), createConverter());
		assertThat(extractor.messageAndStackTrace(createEvent()))
			.isEqualTo("TestMessage\n\nstacktrace:RuntimeException");
	}

	@Test
	void stackTraceWhenNoPrinterPrintsUsingFallback() {
		Extractor extractor = new Extractor(null, createConverter());
		assertThat(extractor.stackTrace(createEvent())).contains("java.lang.RuntimeException: Boom!");
	}

	@Test
	void stackTraceWhenNoPrinterPrintsUsingPrinter() {
		Extractor extractor = new Extractor(new SimpleStackTracePrinter(), createConverter());
		assertThat(extractor.stackTrace(createEvent())).isEqualTo("stacktrace:RuntimeException");
	}

	private ThrowableProxyConverter createConverter() {
		ThrowableProxyConverter converter = new ThrowableProxyConverter();
		converter.start();
		return converter;
	}

	private ILoggingEvent createEvent() {
		LoggingEvent event = new LoggingEvent();
		event.setMessage("TestMessage");
		event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom!")));
		return event;
	}

}
