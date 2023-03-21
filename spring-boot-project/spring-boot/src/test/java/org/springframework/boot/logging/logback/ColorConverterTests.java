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

package org.springframework.boot.logging.logback;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ColorConverter}.
 *
 * @author Phillip Webb
 * @author Krzysztof Krason
 */
class ColorConverterTests {

	private final ColorConverter converter = new ColorConverter();

	private final LoggingEvent event = new LoggingEvent();

	private final String in = "in";

	@BeforeAll
	static void setupAnsi() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
	}

	@AfterAll
	static void resetAnsi() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
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
		this.converter.setOptionList(Collections.singletonList(color));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo(escapeSeq);
	}

	@Test
	void highlightError() {
		this.event.setLevel(Level.ERROR);
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	void highlightWarn() {
		this.event.setLevel(Level.WARN);
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[33min\033[0;39m");
	}

	@Test
	void highlightDebug() {
		this.event.setLevel(Level.DEBUG);
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[32min\033[0;39m");
	}

	@Test
	void highlightTrace() {
		this.event.setLevel(Level.TRACE);
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[32min\033[0;39m");
	}

}
