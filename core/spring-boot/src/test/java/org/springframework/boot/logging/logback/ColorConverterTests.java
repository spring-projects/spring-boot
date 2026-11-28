/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.pattern.LiteralConverter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ColorConverter}.
 *
 * @author Phillip Webb
 * @author Jan Mohr
 * @author Diego Magalhães
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

	@ParameterizedTest
	@MethodSource("optionsAndLevels")
	void convertWhenAnsiDisabledReturnsChildOutput(List<String> options, Level level) {
		ColorConverter converter = startConverter(options, this.in);
		this.event.setLevel(level);
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		try {
			assertThat(converter.convert(this.event)).isEqualTo("in");
		}
		finally {
			AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		}
	}

	@Test
	void convertWhenAnsiEnabledEncodesChildOutput() {
		this.event.setLevel(Level.INFO);
		assertThat(startConverter(Collections.emptyList(), this.in).convert(this.event))
			.isEqualTo("\033[32min\033[0;39m");
		assertThat(startConverter(List.of("faint"), this.in).convert(this.event)).isEqualTo("\033[2min\033[0;39m");
		assertThat(startConverter(List.of("bold,red"), this.in).convert(this.event))
			.isEqualTo("\033[1;31min\033[0;39m");
		assertThat(startConverter(List.of("bold", "red"), this.in).convert(this.event))
			.isEqualTo("\033[1;31min\033[0;39m");
	}

	@Test
	void convertWhenNoChildOutput() {
		ColorConverter converter = startConverter(Collections.emptyList(), null);
		this.event.setLevel(Level.ERROR);
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		try {
			assertThat(converter.convert(this.event)).isEmpty();
		}
		finally {
			AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		}
		assertThat(converter.convert(this.event)).isEqualTo("\033[31m\033[0;39m");
	}

	static Stream<Arguments> optionsAndLevels() {
		List<List<String>> options = List.of(Collections.emptyList(), List.of("faint"), List.of("magenta"),
				List.of("cyan"), List.of("bold,red"), List.of("bold", "red"));
		List<Level> levels = List.of(Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE);
		return options.stream().flatMap((option) -> levels.stream().map((level) -> Arguments.of(option, level)));
	}

	private ColorConverter startConverter(List<String> options, @Nullable String childOutput) {
		ColorConverter converter = new ColorConverter();
		converter.setContext(new LoggerContext());
		converter.setOptionList(options);
		if (childOutput != null) {
			converter.setChildConverter(new LiteralConverter<>(childOutput));
		}
		converter.start();
		return converter;
	}

	private void startWithOptions(String... options) {
		this.converter.setOptionList(Arrays.asList(options));
		this.converter.start();
	}

}
