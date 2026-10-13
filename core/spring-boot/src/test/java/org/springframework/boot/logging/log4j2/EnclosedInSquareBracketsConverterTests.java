/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.log4j2.ColorConverterTests.TestLogEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnclosedInSquareBracketsConverter}.
 *
 * @author Phillip Webb
 */
class EnclosedInSquareBracketsConverterTests {

	private final TestLogEvent event = new TestLogEvent();

	@Test
	void transformWhenEmpty() {
		StringBuilder output = new StringBuilder();
		newConverter("").format(this.event, output);
		assertThat(output).hasToString("");
	}

	@Test
	void transformWhenName() {
		StringBuilder output = new StringBuilder();
		newConverter("My Application").format(this.event, output);
		assertThat(output).hasToString("[My Application] ");
	}

	private EnclosedInSquareBracketsConverter newConverter(String in) {
		EnclosedInSquareBracketsConverter converter = EnclosedInSquareBracketsConverter.newInstance(null,
				new String[] { in });
		assertThat(converter).isNotNull();
		return converter;
	}

}
