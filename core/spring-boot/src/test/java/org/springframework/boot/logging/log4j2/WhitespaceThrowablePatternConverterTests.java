/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WhitespaceThrowablePatternConverter}.
 *
 * @author Vladimir Tsanev
 */
class WhitespaceThrowablePatternConverterTests {

	private final LogEventPatternConverter converter = WhitespaceThrowablePatternConverter
		.newInstance(new DefaultConfiguration(), new String[] {});

	@Test
	void noStackTrace() {
		LogEvent event = Log4jLogEvent.newBuilder().build();
		StringBuilder builder = new StringBuilder();
		this.converter.format(event, builder);
		assertThat(builder).isEmpty();
	}

	@Test
	void withStackTrace() {
		LogEvent event = Log4jLogEvent.newBuilder().setThrown(new Exception()).build();
		StringBuilder builder = new StringBuilder();
		this.converter.format(event, builder);
		assertThat(builder.toString()).startsWith(System.lineSeparator()).endsWith(System.lineSeparator());
	}

}
