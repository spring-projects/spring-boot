/*
 * Copyright 2012-2019 the original author or authors.
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
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage()).isEqualTo("'colorCode' must be between 0 and 255.");
		}
	}

}
