/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ansi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Ansi8BitColor}.
 *
 * @author Toshiaki Maki
 * @author Phillip Webb
 */
class Ansi8BitColorTests {

	@Test
	void toStringWhenForegroundAddsCorrectPrefix() {
		assertThat(Ansi8BitColor.foreground(208)).hasToString("38;5;208");
	}

	@Test
	void toStringWhenBackgroundAddsCorrectPrefix() {
		assertThat(Ansi8BitColor.background(208)).hasToString("48;5;208");
	}

	@Test
	void foregroundWhenOutsideBoundsThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Ansi8BitColor.foreground(-1))
			.withMessage("'code' must be between 0 and 255");
		assertThatIllegalArgumentException().isThrownBy(() -> Ansi8BitColor.foreground(256))
			.withMessage("'code' must be between 0 and 255");
	}

	@Test
	void backgroundWhenOutsideBoundsThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Ansi8BitColor.background(-1))
			.withMessage("'code' must be between 0 and 255");
		assertThatIllegalArgumentException().isThrownBy(() -> Ansi8BitColor.background(256))
			.withMessage("'code' must be between 0 and 255");
	}

	@Test
	void equalsAndHashCode() {
		Ansi8BitColor one = Ansi8BitColor.foreground(123);
		Ansi8BitColor two = Ansi8BitColor.foreground(123);
		Ansi8BitColor three = Ansi8BitColor.background(123);
		assertThat(one).hasSameHashCodeAs(two);
		assertThat(one).isEqualTo(one).isEqualTo(two).isNotEqualTo(three).isNotNull().isNotEqualTo("foo");
	}

}
