/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.Arrays;

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
 * @author Jan Mohr
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
		startWithOptions("black");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[30min\033[0;39m");
	}

	@Test
	void white() {
		startWithOptions("white");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[37min\033[0;39m");
	}

	@Test
	void faint() {
		startWithOptions("faint");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[2min\033[0;39m");
	}

	@Test
	void red() {
		startWithOptions("red");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[31min\033[0;39m");
	}

	@Test
	void green() {
		startWithOptions("green");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[32min\033[0;39m");
	}

	@Test
	void yellow() {
		startWithOptions("yellow");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[33min\033[0;39m");
	}

	@Test
	void blue() {
		startWithOptions("blue");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[34min\033[0;39m");
	}

	@Test
	void magenta() {
		startWithOptions("magenta");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[35min\033[0;39m");
	}

	@Test
	void cyan() {
		startWithOptions("cyan");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[36min\033[0;39m");
	}

	@Test
	void brightBlack() {
		startWithOptions("bright_black");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[90min\033[0;39m");
	}

	@Test
	void brightWhite() {
		startWithOptions("bright_white");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[97min\033[0;39m");
	}

	@Test
	void brightRed() {
		startWithOptions("bright_red");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[91min\033[0;39m");
	}

	@Test
	void brightGreen() {
		startWithOptions("bright_green");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[92min\033[0;39m");
	}

	@Test
	void brightYellow() {
		startWithOptions("bright_yellow");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[93min\033[0;39m");
	}

	@Test
	void brightBlue() {
		startWithOptions("bright_blue");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[94min\033[0;39m");
	}

	@Test
	void brightMagenta() {
		startWithOptions("bright_magenta");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[95min\033[0;39m");
	}

	@Test
	void brightCyan() {
		startWithOptions("bright_cyan");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[96min\033[0;39m");
	}

	@Test
	void bold() {
		startWithOptions("bold");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[1min\033[0;39m");
	}

	@Test
	void italic() {
		startWithOptions("italic");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[3min\033[0;39m");
	}

	@Test
	void underline() {
		startWithOptions("underline");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[4min\033[0;39m");
	}

	@Test
	void reverse() {
		startWithOptions("reverse");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[7min\033[0;39m");
	}

	@Test
	void bgRed() {
		startWithOptions("bg_red");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[41min\033[0;39m");
	}

	@Test
	void bgGreen() {
		startWithOptions("bg_green");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[42min\033[0;39m");
	}

	@Test
	void bgYellow() {
		startWithOptions("bg_yellow");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[43min\033[0;39m");
	}

	@Test
	void bgBlue() {
		startWithOptions("bg_blue");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[44min\033[0;39m");
	}

	@Test
	void bgBrightRed() {
		startWithOptions("bg_bright_red");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[101min\033[0;39m");
	}

	@Test
	void multipleStylesCommaSeparated() {
		startWithOptions("bold, red");
		String out = this.converter.transform(this.event, this.in);
		assertThat(out).isEqualTo("\033[1;31min\033[0;39m");
	}

	@Test
	void multipleStyles() {
		startWithOptions("bold", "red");
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

	private void startWithOptions(String... options) {
		this.converter.setOptionList(Arrays.asList(options));
		this.converter.start();
	}

}
