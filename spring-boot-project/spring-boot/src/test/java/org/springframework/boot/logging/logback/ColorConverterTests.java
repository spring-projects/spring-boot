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
	void white() {
		this.converter.setOptionList(Collections.singletonList("white"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[37m");
	}

	@Test
	void black() {
		this.converter.setOptionList(Collections.singletonList("black"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[30m");
	}

	@Test
	void brightblack() {
		this.converter.setOptionList(Collections.singletonList("bright_black"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[30;1m");
	}

	@Test
	void brightred() {
		this.converter.setOptionList(Collections.singletonList("bright_red"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[31;1m");
	}

	@Test
	void brightgreen() {
		this.converter.setOptionList(Collections.singletonList("bright_green"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[32;1m");
	}	

	@Test
	void brightyellow() {
		this.converter.setOptionList(Collections.singletonList("bright_yellow"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[33;1m");
	}

	@Test
	void brightblue() {
		this.converter.setOptionList(Collections.singletonList("bright_blue"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[34;1m");
	}

	@Test
	void brightmagenta() {
		this.converter.setOptionList(Collections.singletonList("bright_magneta"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[35;1m");
	}

	@Test
	void brightcyan() {
		this.converter.setOptionList(Collections.singletonList("bright_cyan"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[36;1m");
	}

	@Test
	void brightwhite() {
		this.converter.setOptionList(Collections.singletonList("bright_white"));
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\u001B[37;1m");
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
