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
		assertThat(findClosest4Bit(0x000000).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BLACK);
		assertThat(findClosest4Bit(0xAA0000).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.RED);
		assertThat(findClosest4Bit(0x00AA00).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.GREEN);
		assertThat(findClosest4Bit(0xAA5500).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.YELLOW);
		assertThat(findClosest4Bit(0x0000AA).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BLUE);
		assertThat(findClosest4Bit(0xAA00AA).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.MAGENTA);
		assertThat(findClosest4Bit(0x00AAAA).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.CYAN);
		assertThat(findClosest4Bit(0xAAAAAA).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.WHITE);
		assertThat(findClosest4Bit(0x555555).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_BLACK);
		assertThat(findClosest4Bit(0xFF5555).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_RED);
		assertThat(findClosest4Bit(0x55FF00).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_GREEN);
		assertThat(findClosest4Bit(0xFFFF55).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_YELLOW);
		assertThat(findClosest4Bit(0x5555FF).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_BLUE);
		assertThat(findClosest4Bit(0xFF55FF).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_MAGENTA);
		assertThat(findClosest4Bit(0x55FFFF).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_CYAN);
		assertThat(findClosest4Bit(0xFFFFFF).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_WHITE);
	}

	@Test
	void findClosest4BitWhenExactMatchShouldReturnAnsiBackground() {
		assertThat(findClosest4Bit(0x000000).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BLACK);
		assertThat(findClosest4Bit(0xAA0000).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.RED);
		assertThat(findClosest4Bit(0x00AA00).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.GREEN);
		assertThat(findClosest4Bit(0xAA5500).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.YELLOW);
		assertThat(findClosest4Bit(0x0000AA).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BLUE);
		assertThat(findClosest4Bit(0xAA00AA).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.MAGENTA);
		assertThat(findClosest4Bit(0x00AAAA).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.CYAN);
		assertThat(findClosest4Bit(0xAAAAAA).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.WHITE);
		assertThat(findClosest4Bit(0x555555).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_BLACK);
		assertThat(findClosest4Bit(0xFF5555).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_RED);
		assertThat(findClosest4Bit(0x55FF00).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_GREEN);
		assertThat(findClosest4Bit(0xFFFF55).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_YELLOW);
		assertThat(findClosest4Bit(0x5555FF).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_BLUE);
		assertThat(findClosest4Bit(0xFF55FF).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_MAGENTA);
		assertThat(findClosest4Bit(0x55FFFF).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_CYAN);
		assertThat(findClosest4Bit(0xFFFFFF).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_WHITE);
	}

	@Test
	void getClosest4BitWhenCloseShouldReturnAnsiColor() {
		assertThat(findClosest4Bit(0x292424).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BLACK);
		assertThat(findClosest4Bit(0x8C1919).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.RED);
		assertThat(findClosest4Bit(0x0BA10B).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.GREEN);
		assertThat(findClosest4Bit(0xB55F09).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.YELLOW);
		assertThat(findClosest4Bit(0x0B0BA1).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BLUE);
		assertThat(findClosest4Bit(0xA312A3).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.MAGENTA);
		assertThat(findClosest4Bit(0x0BB5B5).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.CYAN);
		assertThat(findClosest4Bit(0xBAB6B6).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.WHITE);
		assertThat(findClosest4Bit(0x615A5A).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_BLACK);
		assertThat(findClosest4Bit(0xF23333).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_RED);
		assertThat(findClosest4Bit(0x55E80C).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_GREEN);
		assertThat(findClosest4Bit(0xF5F54C).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_YELLOW);
		assertThat(findClosest4Bit(0x5656F0).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_BLUE);
		assertThat(findClosest4Bit(0xFA50FA).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_MAGENTA);
		assertThat(findClosest4Bit(0x56F5F5).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_CYAN);
		assertThat(findClosest4Bit(0xEDF5F5).toAnsiElement(ForeOrBack.FORE)).isEqualTo(AnsiColor.BRIGHT_WHITE);
	}

	@Test
	void getClosest4BitWhenCloseShouldReturnAnsiBackground() {
		assertThat(findClosest4Bit(0x292424).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BLACK);
		assertThat(findClosest4Bit(0x8C1919).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.RED);
		assertThat(findClosest4Bit(0x0BA10B).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.GREEN);
		assertThat(findClosest4Bit(0xB55F09).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.YELLOW);
		assertThat(findClosest4Bit(0x0B0BA1).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BLUE);
		assertThat(findClosest4Bit(0xA312A3).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.MAGENTA);
		assertThat(findClosest4Bit(0x0BB5B5).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.CYAN);
		assertThat(findClosest4Bit(0xBAB6B6).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.WHITE);
		assertThat(findClosest4Bit(0x615A5A).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_BLACK);
		assertThat(findClosest4Bit(0xF23333).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_RED);
		assertThat(findClosest4Bit(0x55E80C).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_GREEN);
		assertThat(findClosest4Bit(0xF5F54C).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_YELLOW);
		assertThat(findClosest4Bit(0x5656F0).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_BLUE);
		assertThat(findClosest4Bit(0xFA50FA).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_MAGENTA);
		assertThat(findClosest4Bit(0x56F5F5).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_CYAN);
		assertThat(findClosest4Bit(0xEDF5F5).toAnsiElement(ForeOrBack.BACK)).isEqualTo(AnsiBackground.BRIGHT_WHITE);
	}

	@Test
	void findClosest8BitWhenExactMatchShouldReturnAnsiColor() {
		assertThat(findClosest8Bit(0x000000).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(0));
		assertThat(findClosest8Bit(0xFFFFFF).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(15));
		assertThat(findClosest8Bit(0xFF00FF).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(13));
		assertThat(findClosest8Bit(0x008700).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(28));
		assertThat(findClosest8Bit(0xAF8700).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(136));
	}

	@Test
	void findClosest8BitWhenExactMatchShouldReturnAnsiBackground() {
		assertThat(findClosest8Bit(0x000000).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(0));
		assertThat(findClosest8Bit(0xFFFFFF).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(15));
		assertThat(findClosest8Bit(0xFF00FF).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(13));
		assertThat(findClosest8Bit(0x008700).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(28));
		assertThat(findClosest8Bit(0xAF8700).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(136));
	}

	@Test
	void getClosest8BitWhenCloseShouldReturnAnsiColor() {
		assertThat(findClosest8Bit(0x000001).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(0));
		assertThat(findClosest8Bit(0xFFFFFE).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(15));
		assertThat(findClosest8Bit(0xFF00FE).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(13));
		assertThat(findClosest8Bit(0x008701).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(28));
		assertThat(findClosest8Bit(0xAF8701).toAnsiElement(ForeOrBack.FORE)).isEqualTo(Ansi8BitColor.foreground(136));
	}

	@Test
	void getClosest8BitWhenCloseShouldReturnAnsiBackground() {
		assertThat(findClosest8Bit(0x000001).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(0));
		assertThat(findClosest8Bit(0xFFFFFE).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(15));
		assertThat(findClosest8Bit(0xFF00FE).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(13));
		assertThat(findClosest8Bit(0x008701).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(28));
		assertThat(findClosest8Bit(0xAF8701).toAnsiElement(ForeOrBack.BACK)).isEqualTo(Ansi8BitColor.background(136));
	}

	private AnsiColorWrapper findClosest4Bit(int rgb) {
		return findClosest(BitDepth.FOUR, rgb);
	}

	private AnsiColorWrapper findClosest8Bit(int rgb) {
		return findClosest(BitDepth.EIGHT, rgb);
	}

	private AnsiColorWrapper findClosest(BitDepth depth, int rgb) {
		return new AnsiColors(depth).findClosest(new Color(rgb));
	}

}
