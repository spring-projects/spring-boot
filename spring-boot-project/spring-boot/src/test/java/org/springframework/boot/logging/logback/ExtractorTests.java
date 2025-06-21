/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	void stackTraceWhenNoPrinterPrintsUsingLoggingSystem() {
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
