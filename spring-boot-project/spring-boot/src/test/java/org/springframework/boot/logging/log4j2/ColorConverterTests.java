/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ColorConverter}.
 *
 * @author Vladimir Tsanev
 * @author Krzysztof Krason
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

	private ColorConverter newConverter(String styling) {
		return ColorConverter.newInstance(null, new String[] { this.in, styling });
	}

	@TestFactory
	Stream<DynamicTest> colors() {
		return Map.ofEntries(Map.entry("faint", "\033[2min\033[0;39m"), Map.entry("cyan", "\033[36min\033[0;39m"),
				Map.entry("magenta", "\033[35min\033[0;39m"), Map.entry("blue", "\033[34min\033[0;39m"),
				Map.entry("yellow", "\033[33min\033[0;39m"), Map.entry("green", "\033[32min\033[0;39m"),
				Map.entry("red", "\033[31min\033[0;39m"), Map.entry("black", "\033[30min\033[0;39m"),
				Map.entry("white", "\033[37min\033[0;39m"), Map.entry("bright_cyan", "\033[96min\033[0;39m"),
				Map.entry("bright_magenta", "\033[95min\033[0;39m"), Map.entry("bright_blue", "\033[94min\033[0;39m"),
				Map.entry("bright_yellow", "\033[93min\033[0;39m"), Map.entry("bright_green", "\033[92min\033[0;39m"),
				Map.entry("bright_red", "\033[91min\033[0;39m"), Map.entry("bright_black", "\033[90min\033[0;39m"),
				Map.entry("bright_white", "\033[97min\033[0;39m"))
			.entrySet()
			.stream()
			.map((entry) -> DynamicTest.dynamicTest("Test for %s".formatted(entry.getKey()),
					() -> colorValidation(entry.getKey(), entry.getValue())));
	}

	void colorValidation(String color, String escapeSeq) {
		StringBuilder output = new StringBuilder();
		newConverter(color).format(this.event, output);
		assertThat(output).hasToString(escapeSeq);
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
