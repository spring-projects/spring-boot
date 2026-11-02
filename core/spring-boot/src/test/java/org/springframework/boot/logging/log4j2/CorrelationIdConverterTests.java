/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import java.util.Collections;
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
 * @author Diego Magalhães
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

	@Test
	void formatAppendsToExistingBuffer() {
		String traceId = "01234567890123456789012345678901";
		String spanId = "0123456789012345";
		CorrelationIdConverter defaultConverter = CorrelationIdConverter.newInstance(null);
		assertThat(format(defaultConverter, Map.of("traceId", traceId, "spanId", spanId)))
			.isEqualTo("pre|[" + traceId + "-" + spanId + "] ");
		assertThat(format(defaultConverter, Collections.emptyMap())).isEqualTo("pre|[" + " ".repeat(49) + "] ");
		CorrelationIdConverter customConverter = CorrelationIdConverter.newInstance(new String[] { "a(1),b(0),c(3)" });
		assertThat(format(customConverter, Map.of("a", "x", "c", "yy"))).isEqualTo("pre|[x--yy ] ");
	}

	private String format(CorrelationIdConverter converter, Map<String, String> contextData) {
		StringBuilder result = new StringBuilder("pre|");
		converter.format(new MapLogEvent(contextData), result);
		return result.toString();
	}

	static class TestLogEvent extends AbstractLogEvent {

		@Override
		public ReadOnlyStringMap getContextData() {
			return new JdkMapAdapterStringMap(
					Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"), true);
		}

	}

	static class MapLogEvent extends AbstractLogEvent {

		private final Map<String, String> contextData;

		MapLogEvent(Map<String, String> contextData) {
			this.contextData = contextData;
		}

		@Override
		public ReadOnlyStringMap getContextData() {
			return new JdkMapAdapterStringMap(this.contextData, true);
		}

	}

}
