/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ColorConverter}.
 *
 * @author Vladimir Tsanev
 */
class ColorConverterTests {

	private final String in = "in";

	private TestLogEvent event;

	@BeforeAll
	static void setupAnsi() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
	}

	@AfterAll
	static void resetAnsi() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
	}

	@BeforeEach
	void setUp() {
		this.event = new TestLogEvent();
	}

	@Test
	void black() {
		StringBuilder output = new StringBuilder();
		newConverter("black").format(this.event, output);
		assertThat(output).hasToString("\033[30min\033[0;39m");
	}

	@Test
	void white() {
		StringBuilder output = new StringBuilder();
		newConverter("white").format(this.event, output);
		assertThat(output).hasToString("\033[37min\033[0;39m");
	}

	@Test
	void faint() {
		StringBuilder output = new StringBuilder();
		newConverter("faint").format(this.event, output);
		assertThat(output).hasToString("\033[2min\033[0;39m");
	}

	@Test
	void red() {
		StringBuilder output = new StringBuilder();
		newConverter("red").format(this.event, output);
		assertThat(output).hasToString("\033[31min\033[0;39m");
	}

	@Test
	void green() {
		StringBuilder output = new StringBuilder();
		newConverter("green").format(this.event, output);
		assertThat(output).hasToString("\033[32min\033[0;39m");
	}

	@Test
	void yellow() {
		StringBuilder output = new StringBuilder();
		newConverter("yellow").format(this.event, output);
		assertThat(output).hasToString("\033[33min\033[0;39m");
	}

	@Test
	void blue() {
		StringBuilder output = new StringBuilder();
		newConverter("blue").format(this.event, output);
		assertThat(output).hasToString("\033[34min\033[0;39m");
	}

	@Test
	void magenta() {
		StringBuilder output = new StringBuilder();
		newConverter("magenta").format(this.event, output);
		assertThat(output).hasToString("\033[35min\033[0;39m");
	}

	@Test
	void cyan() {
		StringBuilder output = new StringBuilder();
		newConverter("cyan").format(this.event, output);
		assertThat(output).hasToString("\033[36min\033[0;39m");
	}

	@Test
	void brightBlack() {
		StringBuilder output = new StringBuilder();
		newConverter("bright_black").format(this.event, output);
		assertThat(output).hasToString("\033[90min\033[0;39m");
	}

	@Test
	void brightWhite() {
		StringBuilder output = new StringBuilder();
		newConverter("bright_white").format(this.event, output);
		assertThat(output).hasToString("\033[97min\033[0;39m");
	}

	@Test
	void brightRed() {
		StringBuilder output = new StringBuilder();
		newConverter("bright_red").format(this.event, output);
		assertThat(output).hasToString("\033[91min\033[0;39m");
	}

	@Test
	void brightGreen() {
		StringBuilder output = new StringBuilder();
		newConverter("bright_green").format(this.event, output);
		assertThat(output).hasToString("\033[92min\033[0;39m");
	}

	@Test
	void brightYellow() {
		StringBuilder output = new StringBuilder();
		newConverter("bright_yellow").format(this.event, output);
		assertThat(output).hasToString("\033[93min\033[0;39m");
	}

	@Test
	void brightBlue() {
		StringBuilder output = new StringBuilder();
		newConverter("bright_blue").format(this.event, output);
		assertThat(output).hasToString("\033[94min\033[0;39m");
	}

	@Test
	void brightMagenta() {
		StringBuilder output = new StringBuilder();
		newConverter("bright_magenta").format(this.event, output);
		assertThat(output).hasToString("\033[95min\033[0;39m");
	}

	@Test
	void brightCyan() {
		StringBuilder output = new StringBuilder();
		newConverter("bright_cyan").format(this.event, output);
		assertThat(output).hasToString("\033[96min\033[0;39m");
	}

	@Test
	void highlightFatal() {
		this.event.setLevel(Level.FATAL);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output).hasToString("\033[31min\033[0;39m");
	}

	@Test
	void highlightError() {
		this.event.setLevel(Level.ERROR);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output).hasToString("\033[31min\033[0;39m");
	}

	@Test
	void highlightWarn() {
		this.event.setLevel(Level.WARN);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output).hasToString("\033[33min\033[0;39m");
	}

	@Test
	void highlightDebug() {
		this.event.setLevel(Level.DEBUG);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output).hasToString("\033[32min\033[0;39m");
	}

	@Test
	void highlightTrace() {
		this.event.setLevel(Level.TRACE);
		StringBuilder output = new StringBuilder();
		newConverter(null).format(this.event, output);
		assertThat(output).hasToString("\033[32min\033[0;39m");
	}

	private ColorConverter newConverter(String styling) {
		return ColorConverter.newInstance(null, new String[] { this.in, styling });
	}

	static class TestLogEvent extends AbstractLogEvent {

		private Level level;

		@Override
		public Level getLevel() {
			return this.level;
		}

		void setLevel(Level level) {
			this.level = level;
		}

	}

}
