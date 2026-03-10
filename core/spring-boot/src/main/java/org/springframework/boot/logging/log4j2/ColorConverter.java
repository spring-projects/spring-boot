/*
 * Copyright 2012-present the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Log4j2 {@link LogEventPatternConverter} to color output using the {@link AnsiOutput}
 * class. One or more options can be provided to the converter to set the color and style,
 * or if not specified color styling will be picked based on the logging level.
 *
 * @author Vladimir Tsanev
 * @since 1.3.0
 */
@Plugin(name = "color", category = PatternConverter.CATEGORY)
@ConverterKeys({ "clr", "color" })
public final class ColorConverter extends LogEventPatternConverter {

	private static final Map<String, AnsiElement> ELEMENTS;

	static {
		Map<String, AnsiElement> ansiElements = new HashMap<>();
		Arrays.stream(AnsiColor.values())
			.filter((color) -> color != AnsiColor.DEFAULT)
			.forEach((color) -> ansiElements.put(color.name().toLowerCase(Locale.ROOT), color));
		Arrays.stream(AnsiBackground.values())
			.filter((bg) -> bg != AnsiBackground.DEFAULT)
			.forEach((bg) -> ansiElements.put("bg_" + bg.name().toLowerCase(Locale.ROOT), bg));
		ansiElements.put("bold", AnsiStyle.BOLD);
		ansiElements.put("faint", AnsiStyle.FAINT);
		ansiElements.put("italic", AnsiStyle.ITALIC);
		ansiElements.put("underline", AnsiStyle.UNDERLINE);
		ELEMENTS = Collections.unmodifiableMap(ansiElements);
	}

	private static final Map<Integer, AnsiElement> LEVELS;

	static {
		Map<Integer, AnsiElement> ansiLevels = new HashMap<>();
		ansiLevels.put(Level.FATAL.intLevel(), AnsiColor.RED);
		ansiLevels.put(Level.ERROR.intLevel(), AnsiColor.RED);
		ansiLevels.put(Level.WARN.intLevel(), AnsiColor.YELLOW);
		LEVELS = Collections.unmodifiableMap(ansiLevels);
	}

	private final List<PatternFormatter> formatters;

	private final AnsiElement @Nullable [] stylings;

	private ColorConverter(List<PatternFormatter> formatters, AnsiElement @Nullable [] stylings) {
		super("style", "style");
		this.formatters = formatters;
		this.stylings = stylings;
	}

	@Override
	public boolean handlesThrowable() {
		for (PatternFormatter formatter : this.formatters) {
			if (formatter.handlesThrowable()) {
				return true;
			}
		}
		return super.handlesThrowable();
	}

	@Override
	public void format(LogEvent event, StringBuilder toAppendTo) {
		StringBuilder buf = new StringBuilder();
		for (PatternFormatter formatter : this.formatters) {
			formatter.format(event, buf);
		}
		if (!buf.isEmpty()) {
			if (this.stylings != null && this.stylings.length > 0) {
				appendAnsiString(toAppendTo, buf.toString(), this.stylings);
			}
			else {
				// Assume highlighting
				AnsiElement element = LEVELS.get(event.getLevel().intLevel());
				element = (element != null) ? element : AnsiColor.GREEN;
				appendAnsiString(toAppendTo, buf.toString(), element);
			}
		}
	}

	private void appendAnsiString(StringBuilder toAppendTo, String in, AnsiElement... elements) {
		Object[] args = new Object[elements.length + 1];
		for (int i = 0; i < elements.length; i++) {
			args[i] = elements[i];
		}
		args[elements.length] = in;
		toAppendTo.append(AnsiOutput.toString(args));
	}

	/**
	 * Creates a new instance of the class. Required by Log4J2.
	 * @param config the configuration
	 * @param options the options
	 * @return a new instance, or {@code null} if the options are invalid
	 */
	public static @Nullable ColorConverter newInstance(@Nullable Configuration config, @Nullable String[] options) {
		if (options.length < 1) {
			LOGGER.error("Incorrect number of options on style. Expected at least 1, received {}", options.length);
			return null;
		}
		if (options[0] == null) {
			LOGGER.error("No pattern supplied on style");
			return null;
		}
		PatternParser parser = PatternLayout.createPatternParser(config);
		List<PatternFormatter> formatters = parser.parse(options[0]);
		AnsiElement[] elements = Arrays.stream(options, 1, options.length)
			.map(ELEMENTS::get)
			.filter(Objects::nonNull)
			.toArray(AnsiElement[]::new);
		return new ColorConverter(formatters, (elements.length > 0) ? elements : null);
	}

}
