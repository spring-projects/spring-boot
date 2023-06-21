/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CorrelationIdConverter}.
 *
 * @author Phillip Webb
 */
class CorrelationIdConverterTests {

	private final CorrelationIdConverter converter;

	private final LoggingEvent event = new LoggingEvent();

	CorrelationIdConverterTests() {
		this.converter = new CorrelationIdConverter();
		this.converter.setContext(new LoggerContext());
	}

	@Test
	void defaultPattern() {
		addMdcProperties(this.event);
		this.converter.start();
		String converted = this.converter.convert(this.event);
		this.converter.stop();
		assertThat(converted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void customPattern() {
		this.converter.setOptionList(List.of("traceId(0)", "spanId(0)"));
		addMdcProperties(this.event);
		this.converter.start();
		String converted = this.converter.convert(this.event);
		this.converter.stop();
		assertThat(converted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
	}

	private void addMdcProperties(LoggingEvent event) {
		event.setMDCPropertyMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
	}

}
