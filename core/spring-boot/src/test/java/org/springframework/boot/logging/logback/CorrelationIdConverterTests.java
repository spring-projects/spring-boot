/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.Collections;
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
 * @author Diego Magalhães
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

	@Test
	void convertWhenMdcHasEdgeValues() {
		String traceId = "01234567890123456789012345678901";
		String spanId = "0123456789012345";
		assertThat(convert(Map.of("traceId", traceId, "spanId", spanId)))
			.isEqualTo("[" + traceId + "-" + spanId + "] ");
		assertThat(convert(Collections.emptyMap())).isEqualTo("[" + " ".repeat(49) + "] ");
		assertThat(convert(Map.of("spanId", spanId))).isEqualTo("[" + " ".repeat(32) + "-" + spanId + "] ");
		assertThat(convert(Map.of("a", "x", "c", "yy"), "a(1)", "b(0)", "c(3)")).isEqualTo("[x--yy ] ");
	}

	private String convert(Map<String, String> mdc, String... options) {
		CorrelationIdConverter converter = new CorrelationIdConverter();
		converter.setContext(new LoggerContext());
		if (options.length > 0) {
			converter.setOptionList(List.of(options));
		}
		converter.start();
		LoggingEvent event = new LoggingEvent();
		event.setMDCPropertyMap(mdc);
		return converter.convert(event);
	}

	private void addMdcProperties(LoggingEvent event) {
		event.setMDCPropertyMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
	}

}
