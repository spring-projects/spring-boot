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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
 * class. One or more styling options can be provided to the converter, or if not
 * specified color styling will be picked based on the logging level. Supported options
 * include foreground colors (e.g. {@code red}, {@code bright_blue}), background colors
 * (e.g. {@code bg_red}, {@code bg_bright_green}), and text styles (e.g. {@code bold},
 * {@code underline}, {@code reverse}).
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
		// Foreground colors (e.g. "red", "bright_blue")
		Arrays.stream(AnsiColor.values())
			.filter((color) -> color != AnsiColor.DEFAULT)
			.forEach((color) -> ansiElements.put(color.name().toLowerCase(Locale.ROOT), color));
		// Text styles (e.g. "bold", "italic", "underline", "reverse", "faint", "normal")
		Arrays.stream(AnsiStyle.values())
			.forEach((style) -> ansiElements.put(style.name().toLowerCase(Locale.ROOT), style));
		// Background colors with "bg_" prefix (e.g. "bg_red", "bg_bright_blue")
		Arrays.stream(AnsiBackground.values())
			.filter((bg) -> bg != AnsiBackground.DEFAULT)
			.forEach((bg) -> ansiElements.put("bg_" + bg.name().toLowerCase(Locale.ROOT), bg));
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

	private final List<AnsiElement> stylings;

	private ColorConverter(List<PatternFormatter> formatters, List<AnsiElement> stylings) {
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
			if (this.stylings.isEmpty()) {
				// Assume highlighting
				AnsiElement element = LEVELS.get(event.getLevel().intLevel());
				element = (element != null) ? element : AnsiColor.GREEN;
				appendAnsiString(toAppendTo, buf.toString(), element);
			}
			else {
				appendAnsiString(toAppendTo, buf.toString(), this.stylings.toArray(new AnsiElement[0]));
			}
		}
	}

	protected void appendAnsiString(StringBuilder toAppendTo, String in, AnsiElement element) {
		toAppendTo.append(AnsiOutput.toString(element, in));
	}

	protected void appendAnsiString(StringBuilder toAppendTo, String in, AnsiElement... elements) {
		Object[] ansiParams = new Object[elements.length + 1];
		System.arraycopy(elements, 0, ansiParams, 0, elements.length);
		ansiParams[elements.length] = in;
		toAppendTo.append(AnsiOutput.toString(ansiParams));
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
		List<AnsiElement> stylings = new ArrayList<>();
		for (int i = 1; i < options.length; i++) {
			if (options[i] != null) {
				String[] optionParts = options[i].split(",");
				for (String optionPart : optionParts) {
					AnsiElement element = ELEMENTS.get(optionPart.trim().toLowerCase(Locale.ROOT));
					if (element != null) {
						stylings.add(element);
					}
				}
			}
		}
		return new ColorConverter(formatters, stylings);
	}

}
