/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggerConfigurationComparator}.
 *
 * @author Ben Hale
 */
class LoggerConfigurationComparatorTests {

	private final LoggerConfigurationComparator comparator = new LoggerConfigurationComparator("ROOT");

	@Test
	void rootLoggerFirst() {
		LoggerConfiguration first = new LoggerConfiguration("ROOT", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isLessThan(0);
	}

	@Test
	void rootLoggerSecond() {
		LoggerConfiguration first = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("ROOT", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isGreaterThan(0);
	}

	@Test
	void rootLoggerFirstEmpty() {
		LoggerConfiguration first = new LoggerConfiguration("ROOT", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isLessThan(0);
	}

	@Test
	void rootLoggerSecondEmpty() {
		LoggerConfiguration first = new LoggerConfiguration("", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("ROOT", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isGreaterThan(0);
	}

	@Test
	void lexicalFirst() {
		LoggerConfiguration first = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("bravo", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isLessThan(0);
	}

	@Test
	void lexicalSecond() {
		LoggerConfiguration first = new LoggerConfiguration("bravo", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isGreaterThan(0);
	}

	@Test
	void lexicalEqual() {
		LoggerConfiguration first = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		LoggerConfiguration second = new LoggerConfiguration("alpha", null, LogLevel.OFF);
		assertThat(this.comparator.compare(first, second)).isZero();
	}

}
