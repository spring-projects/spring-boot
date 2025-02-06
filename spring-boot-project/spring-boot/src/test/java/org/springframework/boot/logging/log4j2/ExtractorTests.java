/*
 * Copyright 2012-2025 the original author or authors.
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
