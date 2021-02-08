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

import java.awt.Color;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ansi.AnsiColors.BitDepth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnsiColors}.
 *
 * @author Phillip Webb
 */
class AnsiColorsTests {

	@Test
	void findClosest4BitWhenExactMatchShouldReturnAnsiColor() {
		assertThat(findClosest4Bit(0x000000)).isEqualTo(AnsiColor.BLACK);
		assertThat(findClosest4Bit(0xAA0000)).isEqualTo(AnsiColor.RED);
		assertThat(findClosest4Bit(0x00AA00)).isEqualTo(AnsiColor.GREEN);
		assertThat(findClosest4Bit(0xAA5500)).isEqualTo(AnsiColor.YELLOW);
		assertThat(findClosest4Bit(0x0000AA)).isEqualTo(AnsiColor.BLUE);
		assertThat(findClosest4Bit(0xAA00AA)).isEqualTo(AnsiColor.MAGENTA);
		assertThat(findClosest4Bit(0x00AAAA)).isEqualTo(AnsiColor.CYAN);
		assertThat(findClosest4Bit(0xAAAAAA)).isEqualTo(AnsiColor.WHITE);
		assertThat(findClosest4Bit(0x555555)).isEqualTo(AnsiColor.BRIGHT_BLACK);
		assertThat(findClosest4Bit(0xFF5555)).isEqualTo(AnsiColor.BRIGHT_RED);
		assertThat(findClosest4Bit(0x55FF00)).isEqualTo(AnsiColor.BRIGHT_GREEN);
		assertThat(findClosest4Bit(0xFFFF55)).isEqualTo(AnsiColor.BRIGHT_YELLOW);
		assertThat(findClosest4Bit(0x5555FF)).isEqualTo(AnsiColor.BRIGHT_BLUE);
		assertThat(findClosest4Bit(0xFF55FF)).isEqualTo(AnsiColor.BRIGHT_MAGENTA);
		assertThat(findClosest4Bit(0x55FFFF)).isEqualTo(AnsiColor.BRIGHT_CYAN);
		assertThat(findClosest4Bit(0xFFFFFF)).isEqualTo(AnsiColor.BRIGHT_WHITE);
	}

	@Test
	void getClosest4BitWhenCloseShouldReturnAnsiColor() {
		assertThat(findClosest4Bit(0x292424)).isEqualTo(AnsiColor.BLACK);
		assertThat(findClosest4Bit(0x8C1919)).isEqualTo(AnsiColor.RED);
		assertThat(findClosest4Bit(0x0BA10B)).isEqualTo(AnsiColor.GREEN);
		assertThat(findClosest4Bit(0xB55F09)).isEqualTo(AnsiColor.YELLOW);
		assertThat(findClosest4Bit(0x0B0BA1)).isEqualTo(AnsiColor.BLUE);
		assertThat(findClosest4Bit(0xA312A3)).isEqualTo(AnsiColor.MAGENTA);
		assertThat(findClosest4Bit(0x0BB5B5)).isEqualTo(AnsiColor.CYAN);
		assertThat(findClosest4Bit(0xBAB6B6)).isEqualTo(AnsiColor.WHITE);
		assertThat(findClosest4Bit(0x615A5A)).isEqualTo(AnsiColor.BRIGHT_BLACK);
		assertThat(findClosest4Bit(0xF23333)).isEqualTo(AnsiColor.BRIGHT_RED);
		assertThat(findClosest4Bit(0x55E80C)).isEqualTo(AnsiColor.BRIGHT_GREEN);
		assertThat(findClosest4Bit(0xF5F54C)).isEqualTo(AnsiColor.BRIGHT_YELLOW);
		assertThat(findClosest4Bit(0x5656F0)).isEqualTo(AnsiColor.BRIGHT_BLUE);
		assertThat(findClosest4Bit(0xFA50FA)).isEqualTo(AnsiColor.BRIGHT_MAGENTA);
		assertThat(findClosest4Bit(0x56F5F5)).isEqualTo(AnsiColor.BRIGHT_CYAN);
		assertThat(findClosest4Bit(0xEDF5F5)).isEqualTo(AnsiColor.BRIGHT_WHITE);
	}

	@Test
	void findClosest8BitWhenExactMatchShouldReturnAnsiColor() {
		assertThat(findClosest8Bit(0x000000)).isEqualTo(Ansi8BitColor.foreground(0));
		assertThat(findClosest8Bit(0xFFFFFF)).isEqualTo(Ansi8BitColor.foreground(15));
		assertThat(findClosest8Bit(0xFF00FF)).isEqualTo(Ansi8BitColor.foreground(13));
		assertThat(findClosest8Bit(0x008700)).isEqualTo(Ansi8BitColor.foreground(28));
		assertThat(findClosest8Bit(0xAF8700)).isEqualTo(Ansi8BitColor.foreground(136));
	}

	@Test
	void getClosest8BitWhenCloseShouldReturnAnsiColor() {
		assertThat(findClosest8Bit(0x000001)).isEqualTo(Ansi8BitColor.foreground(0));
		assertThat(findClosest8Bit(0xFFFFFE)).isEqualTo(Ansi8BitColor.foreground(15));
		assertThat(findClosest8Bit(0xFF00FE)).isEqualTo(Ansi8BitColor.foreground(13));
		assertThat(findClosest8Bit(0x008701)).isEqualTo(Ansi8BitColor.foreground(28));
		assertThat(findClosest8Bit(0xAF8701)).isEqualTo(Ansi8BitColor.foreground(136));
	}

	private AnsiElement findClosest4Bit(int rgb) {
		return findClosest(BitDepth.FOUR, rgb);
	}

	private AnsiElement findClosest8Bit(int rgb) {
		return findClosest(BitDepth.EIGHT, rgb);
	}

	private AnsiElement findClosest(BitDepth depth, int rgb) {
		return new AnsiColors(depth).findClosest(new Color(rgb));
	}

}
