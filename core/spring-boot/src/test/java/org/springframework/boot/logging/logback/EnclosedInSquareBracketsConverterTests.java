/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.pattern.LiteralConverter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnclosedInSquareBracketsConverter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Diego Magalhães
 */
class EnclosedInSquareBracketsConverterTests {

	private final EnclosedInSquareBracketsConverter converter;

	private final LoggingEvent event = new LoggingEvent();

	EnclosedInSquareBracketsConverterTests() {
		this.converter = new EnclosedInSquareBracketsConverter();
		this.converter.setContext(new LoggerContext());
		this.event.setLoggerContextRemoteView(
				new LoggerContextVO("test", Collections.emptyMap(), System.currentTimeMillis()));
	}

	@Test
	void transformWhenNull() {
		assertThat(this.converter.transform(this.event, null)).isEqualTo("");
	}

	@Test
	void transformWhenEmpty() {
		assertThat(this.converter.transform(this.event, "")).isEqualTo("");
	}

	@Test
	void transformWhenName() {
		assertThat(this.converter.transform(this.event, "My Application")).isEqualTo("[My Application] ");
	}

	@Test
	void transformWhenEmptyFromFirstOption() {
		withLoggedApplicationName("spring", null, () -> {
			this.converter.setOptionList(List.of("spring"));
			this.converter.start();
			String converted = this.converter.convert(this.event);
			assertThat(converted).isEqualTo("");
		});
	}

	@Test
	void transformWhenNameFromFirstOption() {
		withLoggedApplicationName("spring", "boot", () -> {
			this.converter.setOptionList(List.of("spring"));
			this.converter.start();
			String converted = this.converter.convert(this.event);
			assertThat(converted).isEqualTo("[boot] ");
		});
	}

	@ParameterizedTest
	@MethodSource("names")
	void convertWhenChildOutputHasTextEnclosesIt(String name) {
		LoggerContext context = new LoggerContext();
		EnclosedInSquareBracketsConverter converter = startConverter(context, name);
		assertThat(converter.convert(createEvent(context))).isEqualTo("[" + name + "] ");
	}

	@ParameterizedTest
	@MethodSource("names")
	void convertWhenApplicationNameIsContextPropertyEnclosesIt(String name) {
		LoggerContext context = new LoggerContext();
		context.putProperty("APPLICATION_NAME", name);
		EnclosedInSquareBracketsConverter converter = startConverter(context, null, "APPLICATION_NAME");
		assertThat(converter.convert(createEvent(context))).isEqualTo("[" + name + "] ");
	}

	@ParameterizedTest
	@MethodSource("names")
	void convertWhenApplicationNameIsSystemPropertyEnclosesIt(String name) {
		withLoggedApplicationName("spring", name, () -> {
			LoggerContext context = new LoggerContext();
			EnclosedInSquareBracketsConverter converter = startConverter(context, null, "spring");
			assertThat(converter.convert(createEvent(context))).isEqualTo("[" + name + "] ");
		});
	}

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = { "demo, null, '[demo] |msg'", "demo, g, '[demo] [g] |msg'",
			"null, g, '[g] |msg'", "null, null, '|msg'", "'', null, '|msg'", "%s, %n, '[%s] [%n] |msg'" })
	void layoutWithApplicationNameAndGroup(@Nullable String name, @Nullable String group, String expected) {
		LoggerContext context = new LoggerContext();
		if (name != null) {
			context.putProperty("APPLICATION_NAME", name);
		}
		if (group != null) {
			context.putProperty("APPLICATION_GROUP", group);
		}
		PatternLayout layout = new PatternLayout();
		layout.setContext(context);
		layout.getInstanceConverterMap().put("esb", EnclosedInSquareBracketsConverter::new);
		layout.setPattern("%esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}|%m");
		layout.start();
		assertThat(layout.doLayout(createEvent(context))).isEqualTo(expected);
	}

	@Test
	void convertWhenNothingResolvesReturnsEmptyString() {
		LoggerContext context = new LoggerContext();
		assertThat(startConverter(context, null).convert(createEvent(context))).isEqualTo("");
		withLoggedApplicationName("spring", null, () -> {
			EnclosedInSquareBracketsConverter converter = startConverter(context, null, "spring");
			assertThat(converter.convert(createEvent(context))).isEqualTo("");
		});
		context.putProperty("APPLICATION_NAME", "");
		withLoggedApplicationName("APPLICATION_NAME", "sys", () -> {
			EnclosedInSquareBracketsConverter converter = startConverter(context, null, "APPLICATION_NAME");
			assertThat(converter.convert(createEvent(context))).isEqualTo("");
		});
	}

	@Test
	void convertWhenChildOutputHasTextIgnoresOption() {
		LoggerContext context = new LoggerContext();
		context.putProperty("APPLICATION_NAME", "prop");
		EnclosedInSquareBracketsConverter converter = startConverter(context, "child", "APPLICATION_NAME");
		assertThat(converter.convert(createEvent(context))).isEqualTo("[child] ");
	}

	@ParameterizedTest
	@ValueSource(strings = { "tr-TR", "ar-EG", "th-TH-u-nu-thai", "hi-IN-u-nu-deva", "zh-CN" })
	void convertIsLocaleIndependent(String languageTag) {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag(languageTag));
			LoggerContext context = new LoggerContext();
			for (String child : List.of("123", "ITALIC", "i", "\u0130stanbul")) {
				EnclosedInSquareBracketsConverter converter = startConverter(context, child);
				assertThat(converter.convert(createEvent(context))).isEqualTo("[" + child + "] ");
			}
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	static Stream<String> names() {
		return Stream.of("demo", "My Application", " ", "%s", "%n", "%%", "%1$s", "%d", "{}", "{0}", "[x]", "]", "\\",
				"\t", "caf\u00e9", "\u65e5\u672c\u8a9e", "\uD83D\uDE00", "\uD800", "\u001B[31m", "null",
				"x".repeat(10_000));
	}

	private EnclosedInSquareBracketsConverter startConverter(LoggerContext context, @Nullable String child,
			String... options) {
		EnclosedInSquareBracketsConverter converter = new EnclosedInSquareBracketsConverter();
		converter.setContext(context);
		if (child != null) {
			converter.setChildConverter(new LiteralConverter<>(child));
		}
		converter.setOptionList(List.of(options));
		converter.start();
		return converter;
	}

	private LoggingEvent createEvent(LoggerContext context) {
		return new LoggingEvent(getClass().getName(), context.getLogger("test"), Level.INFO, "msg", null, null);
	}

	private void withLoggedApplicationName(String name, @Nullable String value, Runnable action) {
		if (value == null) {
			System.clearProperty(name);
		}
		else {
			System.setProperty(name, value);
		}
		try {
			action.run();
		}
		finally {
			System.clearProperty(name);
		}
	}

}
