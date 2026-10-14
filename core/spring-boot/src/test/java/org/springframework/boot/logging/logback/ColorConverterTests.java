/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.Arrays;
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
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[30min\033[0;39m");
	}

	@Test
	void white() {
		this.converter.setOptionList(Collections.singletonList("white"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[37min\033[0;39m");
	}

	@Test
	void faint() {
		this.converter.setOptionList(Collections.singletonList("faint"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[2min\033[0;39m");
	}

	@Test
	void red() {
		this.converter.setOptionList(Collections.singletonList("red"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	void green() {
		this.converter.setOptionList(Collections.singletonList("green"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[32min\033[0;39m");
	}

	@Test
	void yellow() {
		this.converter.setOptionList(Collections.singletonList("yellow"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[33min\033[0;39m");
	}

	@Test
	void blue() {
		this.converter.setOptionList(Collections.singletonList("blue"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[34min\033[0;39m");
	}

	@Test
	void magenta() {
		this.converter.setOptionList(Collections.singletonList("magenta"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[35min\033[0;39m");
	}

	@Test
	void cyan() {
		this.converter.setOptionList(Collections.singletonList("cyan"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[36min\033[0;39m");
	}

	@Test
	void brightBlack() {
		this.converter.setOptionList(Collections.singletonList("bright_black"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[90min\033[0;39m");
	}

	@Test
	void brightWhite() {
		this.converter.setOptionList(Collections.singletonList("bright_white"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[97min\033[0;39m");
	}

	@Test
	void brightRed() {
		this.converter.setOptionList(Collections.singletonList("bright_red"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[91min\033[0;39m");
	}

	@Test
	void brightGreen() {
		this.converter.setOptionList(Collections.singletonList("bright_green"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[92min\033[0;39m");
	}

	@Test
	void brightYellow() {
		this.converter.setOptionList(Collections.singletonList("bright_yellow"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[93min\033[0;39m");
	}

	@Test
	void brightBlue() {
		this.converter.setOptionList(Collections.singletonList("bright_blue"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[94min\033[0;39m");
	}

	@Test
	void brightMagenta() {
		this.converter.setOptionList(Collections.singletonList("bright_magenta"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[95min\033[0;39m");
	}

	@Test
	void brightCyan() {
		this.converter.setOptionList(Collections.singletonList("bright_cyan"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[96min\033[0;39m");
	}

	@Test
	void bold() {
		this.converter.setOptionList(Collections.singletonList("bold"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[1min\033[0;39m");
	}

	@Test
	void italic() {
		this.converter.setOptionList(Collections.singletonList("italic"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[3min\033[0;39m");
	}

	@Test
	void underline() {
		this.converter.setOptionList(Collections.singletonList("underline"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[4min\033[0;39m");
	}

	@Test
	void reverse() {
		this.converter.setOptionList(Collections.singletonList("reverse"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[7min\033[0;39m");
	}

	@Test
	void bgRed() {
		this.converter.setOptionList(Collections.singletonList("bg_red"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[41min\033[0;39m");
	}

	@Test
	void bgGreen() {
		this.converter.setOptionList(Collections.singletonList("bg_green"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[42min\033[0;39m");
	}

	@Test
	void bgYellow() {
		this.converter.setOptionList(Collections.singletonList("bg_yellow"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[43min\033[0;39m");
	}

	@Test
	void bgBlue() {
		this.converter.setOptionList(Collections.singletonList("bg_blue"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[44min\033[0;39m");
	}

	@Test
	void bgBrightRed() {
		this.converter.setOptionList(Collections.singletonList("bg_bright_red"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[101min\033[0;39m");
	}

	@Test
	void multipleStylesCommaSeparated() {
		this.converter.setOptionList(Collections.singletonList("bold, red"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[1;31min\033[0;39m");
	}

	@Test
	void multipleStyles() {
		this.converter.setOptionList(Arrays.asList("bold", "red"));
		this.converter.start();
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[1;31min\033[0;39m");
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
