/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ansi;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ansi.AnsiOutput.Enabled;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnsiOutput}.
 *
 * @author Phillip Webb
 */
class AnsiOutputTests {

	@BeforeAll
	static void enable() {
		AnsiOutput.setEnabled(Enabled.ALWAYS);
	}

	@AfterAll
	static void reset() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	void encoding() {
		String encoded = AnsiOutput.toString("A", AnsiColor.RED, AnsiStyle.BOLD, "B", AnsiStyle.NORMAL, "D",
				AnsiColor.GREEN, "E", AnsiStyle.FAINT, "F");
		assertThat(encoded).isEqualTo("A[31;1mB[0mD[32mE[2mF[0;39m");
	}

	@Test
	void toStringWhenDisabledAndSingleStringReturnsString() {
		withAnsiDisabled(() -> assertThat(AnsiOutput.toString(AnsiColor.RED, "A")).isEqualTo("A"));
	}

	@Test
	void toStringWhenDisabledAndSeveralAnsiElementsWithSingleStringReturnsString() {
		withAnsiDisabled(() -> assertThat(AnsiOutput.toString(AnsiColor.RED, AnsiStyle.BOLD, "A", AnsiStyle.NORMAL))
			.isEqualTo("A"));
	}

	@Test
	void toStringWhenDisabledAndSeveralStringsRemovesAnsiElements() {
		withAnsiDisabled(() -> assertThat(
				AnsiOutput.toString("A", AnsiColor.RED, AnsiStyle.BOLD, "B", AnsiStyle.NORMAL, "D", AnsiColor.GREEN))
			.isEqualTo("ABD"));
	}

	@Test
	void toStringWhenDisabledAndOnlyAnsiElementsReturnsEmptyString() {
		withAnsiDisabled(() -> assertThat(AnsiOutput.toString(AnsiColor.RED, AnsiStyle.BOLD)).isEmpty());
	}

	@Test
	void toStringWhenDisabledAndHasNonStringElementUsesToString() {
		withAnsiDisabled(() -> assertThat(AnsiOutput.toString(AnsiColor.RED, 42)).isEqualTo("42"));
	}

	private void withAnsiDisabled(Runnable action) {
		AnsiOutput.setEnabled(Enabled.NEVER);
		try {
			action.run();
		}
		finally {
			AnsiOutput.setEnabled(Enabled.ALWAYS);
		}
	}

}
