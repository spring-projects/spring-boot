package org.springframework.boot.ansi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * Tests for {@link Ansi256Color}.
 *
 * @author Toshiaki Maki
 */
class Ansi256ColorTest {

	@Test
	void testForeground() {
		final Ansi256Color ansi256Color = new Ansi256Color.Foreground(208);
		assertThat(ansi256Color.toString()).isEqualTo("38;5;208");
	}

	@Test
	void testBackground() {
		final Ansi256Color ansi256Color = new Ansi256Color.Background(208);
		assertThat(ansi256Color.toString()).isEqualTo("48;5;208");
	}

	@Test
	void testIllegalColorCode() {
		try {
			new Ansi256Color.Foreground(256);
			failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("'colorCode' must be between 0 and 255.");
		}
	}
}