/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import java.util.Map;

import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CorrelationIdConverter}.
 *
 * @author Phillip Webb
 */
class CorrelationIdConverterTests {

	private CorrelationIdConverter converter = CorrelationIdConverter.newInstance(null);

	private final LogEvent event = new TestLogEvent();

	@Test
	void defaultPattern() {
		StringBuilder result = new StringBuilder();
		this.converter.format(this.event, result);
		assertThat(result).hasToString("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void customPattern() {
		this.converter = CorrelationIdConverter.newInstance(new String[] { "traceId(0),spanId(0)" });
		StringBuilder result = new StringBuilder();
		this.converter.format(this.event, result);
		assertThat(result).hasToString("[01234567890123456789012345678901-0123456789012345] ");
	}

	static class TestLogEvent extends AbstractLogEvent {

		@Override
		public ReadOnlyStringMap getContextData() {
			return new JdkMapAdapterStringMap(
					Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"), true);
		}

	}

}
