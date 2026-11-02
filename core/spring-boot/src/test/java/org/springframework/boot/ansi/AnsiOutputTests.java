/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ansi;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.ansi.AnsiOutput.Enabled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link AnsiOutput}.
 *
 * @author Phillip Webb
 * @author Diego Magalhães
 */
class AnsiOutputTests {

	@BeforeAll
	static void enable() {
		AnsiOutput.setEnabled(Enabled.ALWAYS);
	}

	@AfterAll
	static void reset() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	void encoding() {
		String encoded = AnsiOutput.toString("A", AnsiColor.RED, AnsiStyle.BOLD, "B", AnsiStyle.NORMAL, "D",
				AnsiColor.GREEN, "E", AnsiStyle.FAINT, "F");
		assertThat(encoded).isEqualTo("A[31;1mB[0mD[32mE[2mF[0;39m");
	}

	@ParameterizedTest
	@MethodSource("texts")
	@SuppressWarnings("NullAway") // Test null check
	void toStringWhenDisabledAndSingleStringReturnsString(String text) {
		withEnabled(Enabled.NEVER, () -> {
			assertThat(AnsiOutput.toString(AnsiColor.RED, text)).isEqualTo(text);
			assertThat(AnsiOutput.toString(text)).isEqualTo(text);
			assertThat(AnsiOutput.toString(AnsiStyle.BOLD, AnsiColor.RED, text)).isEqualTo(text);
			assertThat(AnsiOutput.toString(text, AnsiColor.RED)).isEqualTo(text);
			assertThat(AnsiOutput.toString(AnsiColor.RED, null, text, null)).isEqualTo(text);
			assertThat(AnsiOutput.toString(Ansi8BitColor.foreground(208), text)).isEqualTo(text);
		});
	}

	@ParameterizedTest
	@MethodSource("texts")
	void toStringWhenEnabledAndSingleStringEncodesString(String text) {
		withEnabled(Enabled.ALWAYS, () -> {
			assertThat(AnsiOutput.toString(AnsiColor.RED, text)).isEqualTo("\033[31m" + text + "\033[0;39m");
			assertThat(AnsiOutput.toString(AnsiStyle.BOLD, AnsiColor.RED, text))
				.isEqualTo("\033[1;31m" + text + "\033[0;39m");
			assertThat(AnsiOutput.toString(text)).isEqualTo(text);
			assertThat(AnsiOutput.toString(text, AnsiColor.RED)).isEqualTo(text + "\033[31;0;39m");
		});
	}

	@Test
	void toStringWhenDisabledAndSeveralStringsJoinsThem() {
		withEnabled(Enabled.NEVER, () -> {
			assertThat(AnsiOutput.toString(AnsiColor.RED, "", "x")).isEqualTo("x");
			assertThat(AnsiOutput.toString("", "x")).isEqualTo("x");
			assertThat(AnsiOutput.toString("x", "")).isEqualTo("x");
			assertThat(AnsiOutput.toString("A", AnsiColor.RED, "B", AnsiStyle.BOLD, "C")).isEqualTo("ABC");
			for (String padding : new String[] { "", "   " }) {
				assertThat(AnsiOutput.toString(AnsiColor.GREEN, " :: Spring Boot :: ", AnsiColor.DEFAULT, padding,
						AnsiStyle.FAINT, " (v1.2.3)"))
					.isEqualTo(" :: Spring Boot :: " + padding + " (v1.2.3)");
			}
		});
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void toStringWhenDisabledAndNoStringReturnsEmptyString() {
		withEnabled(Enabled.NEVER, () -> {
			assertThat(AnsiOutput.toString(new Object[0])).isEmpty();
			assertThat(AnsiOutput.toString(AnsiColor.RED)).isEmpty();
			assertThat(AnsiOutput.toString(AnsiColor.RED, AnsiStyle.BOLD)).isEmpty();
			assertThat(AnsiOutput.toString((Object) null)).isEmpty();
			assertThat(AnsiOutput.toString(AnsiColor.RED, null)).isEmpty();
			assertThat(AnsiOutput.toString(null, null)).isEmpty();
		});
	}

	@Test
	void toStringWhenDisabledAndNotStringUsesStringValue() {
		withEnabled(Enabled.NEVER, () -> {
			assertThat(AnsiOutput.toString(AnsiColor.RED, 42)).isEqualTo("42");
			assertThat(AnsiOutput.toString(AnsiColor.RED, 'c')).isEqualTo("c");
			assertThat(AnsiOutput.toString(AnsiColor.RED, new StringBuilder("sb"))).isEqualTo("sb");
			assertThat(AnsiOutput.toString(AnsiColor.RED, objectWithToString("custom"))).isEqualTo("custom");
			assertThat(AnsiOutput.toString(AnsiColor.RED, objectWithToString(null))).isEqualTo("null");
		});
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void toStringWhenEnabledAndNoStringOrNotString() {
		withEnabled(Enabled.ALWAYS, () -> {
			assertThat(AnsiOutput.toString(new Object[0])).isEmpty();
			assertThat(AnsiOutput.toString(AnsiColor.RED)).isEqualTo("\033[31;0;39m");
			assertThat(AnsiOutput.toString((Object) null)).isEqualTo("null");
			assertThat(AnsiOutput.toString(AnsiColor.RED, null)).isEqualTo("\033[31mnull\033[0;39m");
			assertThat(AnsiOutput.toString(AnsiColor.RED, null, "t", null)).isEqualTo("\033[31mnulltnull\033[0;39m");
			assertThat(AnsiOutput.toString(AnsiColor.RED, 42)).isEqualTo("\033[31m42\033[0;39m");
			assertThat(AnsiOutput.toString(AnsiColor.GREEN, " :: Spring Boot :: ", AnsiColor.DEFAULT, "",
					AnsiStyle.FAINT, " (v1.2.3)"))
				.isEqualTo("\033[32m :: Spring Boot :: \033[39m\033[2m (v1.2.3)\033[0;39m");
			assertThat(AnsiOutput.toString(AnsiColor.GREEN, " :: Spring Boot :: ", AnsiColor.DEFAULT, "   ",
					AnsiStyle.FAINT, " (v1.2.3)"))
				.isEqualTo("\033[32m :: Spring Boot :: \033[39m   \033[2m (v1.2.3)\033[0;39m");
		});
	}

	@ParameterizedTest
	@MethodSource("texts")
	void toStringWhenDetectUsesDetectedOutput(String text) {
		withEnabled(Enabled.DETECT, () -> {
			AnsiOutput.setConsoleAvailable(false);
			try {
				boolean detected = !AnsiOutput.encode(AnsiColor.RED).isEmpty();
				String expected = detected ? "\033[31m" + text + "\033[0;39m" : text;
				assertThat(AnsiOutput.toString(AnsiColor.RED, text)).isEqualTo(expected);
			}
			finally {
				AnsiOutput.setConsoleAvailable(null);
			}
		});
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"AnsiColor,DEFAULT,39",
			"AnsiColor,BLACK,30",
			"AnsiColor,RED,31",
			"AnsiColor,GREEN,32",
			"AnsiColor,YELLOW,33",
			"AnsiColor,BLUE,34",
			"AnsiColor,MAGENTA,35",
			"AnsiColor,CYAN,36",
			"AnsiColor,WHITE,37",
			"AnsiColor,BRIGHT_BLACK,90",
			"AnsiColor,BRIGHT_RED,91",
			"AnsiColor,BRIGHT_GREEN,92",
			"AnsiColor,BRIGHT_YELLOW,93",
			"AnsiColor,BRIGHT_BLUE,94",
			"AnsiColor,BRIGHT_MAGENTA,95",
			"AnsiColor,BRIGHT_CYAN,96",
			"AnsiColor,BRIGHT_WHITE,97",
			"AnsiStyle,NORMAL,0",
			"AnsiStyle,BOLD,1",
			"AnsiStyle,FAINT,2",
			"AnsiStyle,ITALIC,3",
			"AnsiStyle,UNDERLINE,4",
			"AnsiStyle,REVERSE,7",
			"AnsiBackground,DEFAULT,49",
			"AnsiBackground,BLACK,40",
			"AnsiBackground,RED,41",
			"AnsiBackground,GREEN,42",
			"AnsiBackground,YELLOW,43",
			"AnsiBackground,BLUE,44",
			"AnsiBackground,MAGENTA,45",
			"AnsiBackground,CYAN,46",
			"AnsiBackground,WHITE,47",
			"AnsiBackground,BRIGHT_BLACK,100",
			"AnsiBackground,BRIGHT_RED,101",
			"AnsiBackground,BRIGHT_GREEN,102",
			"AnsiBackground,BRIGHT_YELLOW,103",
			"AnsiBackground,BRIGHT_BLUE,104",
			"AnsiBackground,BRIGHT_MAGENTA,105",
			"AnsiBackground,BRIGHT_CYAN,106",
			"AnsiBackground,BRIGHT_WHITE,107",
			"Ansi8BitColor.foreground,0,38;5;0",
			"Ansi8BitColor.foreground,255,38;5;255",
			"Ansi8BitColor.background,208,48;5;208"
	})
	// @formatter:on
	void toStringEncodesEveryElement(String type, String name, String code) {
		AnsiElement element = element(type, name);
		withEnabled(Enabled.ALWAYS,
				() -> assertThat(AnsiOutput.toString(element, "x")).isEqualTo("\033[" + code + "mx\033[0;39m"));
		withEnabled(Enabled.NEVER, () -> assertThat(AnsiOutput.toString(element, "x")).isEqualTo("x"));
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void toStringWhenElementsArrayIsNullThrowsNullPointerException() {
		withEnabled(Enabled.NEVER,
				() -> assertThatNullPointerException().isThrownBy(() -> AnsiOutput.toString((Object[]) null)));
		withEnabled(Enabled.ALWAYS,
				() -> assertThatNullPointerException().isThrownBy(() -> AnsiOutput.toString((Object[]) null)));
	}

	static Stream<String> texts() {
		return Stream.of("", " ", "in", " INFO", "2026-11-01T10:15:30.123+01:00",
				"--- [demo] [nt-jmh-worker-1] [4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7] ",
				"com.example.demo.service.OrderService   ", ":", "null", "%s", "%n%%", "{}", "[x]",
				"\u001B[31mred\u001B[0m", "tab\tnew\nline\r", "\u0000", "caf\u00e9", "\u65E5\u672C\u8A9E",
				"\uD83D\uDE00", "\uD800", "x".repeat(10_000));
	}

	private static AnsiElement element(String type, String name) {
		return switch (type) {
			case "AnsiColor" -> AnsiColor.valueOf(name);
			case "AnsiStyle" -> AnsiStyle.valueOf(name);
			case "AnsiBackground" -> AnsiBackground.valueOf(name);
			case "Ansi8BitColor.foreground" -> Ansi8BitColor.foreground(Integer.parseInt(name));
			case "Ansi8BitColor.background" -> Ansi8BitColor.background(Integer.parseInt(name));
			default -> throw new IllegalArgumentException("Unknown element type '" + type + "'");
		};
	}

	private static void withEnabled(Enabled enabled, Runnable action) {
		AnsiOutput.setEnabled(enabled);
		try {
			action.run();
		}
		finally {
			AnsiOutput.setEnabled(Enabled.ALWAYS);
		}
	}

	@SuppressWarnings("NullAway") // Test null check
	private static Object objectWithToString(@Nullable String value) {
		return new Object() {

			@Override
			public String toString() {
				return value;
			}

		};
	}

}
