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

package org.springframework.boot.logging.logback;

import java.util.Collections;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ColorConverter}.
 *
 * @author Phillip Webb
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

	@Test
	void black() {
		this.converter.setOptionList(Collections.singletonList("black"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[30min\033[0;39m");
	}

	@Test
	void white() {
		this.converter.setOptionList(Collections.singletonList("white"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[37min\033[0;39m");
	}

	@Test
	void faint() {
		this.converter.setOptionList(Collections.singletonList("faint"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[2min\033[0;39m");
	}

	@Test
	void red() {
		this.converter.setOptionList(Collections.singletonList("red"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	void green() {
		this.converter.setOptionList(Collections.singletonList("green"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[32min\033[0;39m");
	}

	@Test
	void yellow() {
		this.converter.setOptionList(Collections.singletonList("yellow"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[33min\033[0;39m");
	}

	@Test
	void blue() {
		this.converter.setOptionList(Collections.singletonList("blue"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[34min\033[0;39m");
	}

	@Test
	void magenta() {
		this.converter.setOptionList(Collections.singletonList("magenta"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[35min\033[0;39m");
	}

	@Test
	void cyan() {
		this.converter.setOptionList(Collections.singletonList("cyan"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[36min\033[0;39m");
	}

	@Test
	void brightBlack() {
		this.converter.setOptionList(Collections.singletonList("bright_black"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[90min\033[0;39m");
	}

	@Test
	void brightWhite() {
		this.converter.setOptionList(Collections.singletonList("bright_white"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[97min\033[0;39m");
	}

	@Test
	void brightRed() {
		this.converter.setOptionList(Collections.singletonList("bright_red"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[91min\033[0;39m");
	}

	@Test
	void brightGreen() {
		this.converter.setOptionList(Collections.singletonList("bright_green"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[92min\033[0;39m");
	}

	@Test
	void brightYellow() {
		this.converter.setOptionList(Collections.singletonList("bright_yellow"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[93min\033[0;39m");
	}

	@Test
	void brightBlue() {
		this.converter.setOptionList(Collections.singletonList("bright_blue"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[94min\033[0;39m");
	}

	@Test
	void brightMagenta() {
		this.converter.setOptionList(Collections.singletonList("bright_magenta"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[95min\033[0;39m");
	}

	@Test
	void brightCyan() {
		this.converter.setOptionList(Collections.singletonList("bright_cyan"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[96min\033[0;39m");
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
