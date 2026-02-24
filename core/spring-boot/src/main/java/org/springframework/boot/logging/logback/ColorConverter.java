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

package org.springframework.boot.logging.logback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Logback {@link CompositeConverter} to color output using the {@link AnsiOutput} class.
 * One or more styling options can be provided to the converter, or if not specified color
 * will be picked based on the logging level. Supported options include foreground colors
 * (e.g. {@code red}, {@code bright_blue}), background colors (e.g. {@code bg_red},
 * {@code bg_bright_green}), and text styles (e.g. {@code bold}, {@code underline},
 * {@code reverse}).
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class ColorConverter extends CompositeConverter<ILoggingEvent> {

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
		ansiLevels.put(Level.ERROR_INTEGER, AnsiColor.RED);
		ansiLevels.put(Level.WARN_INTEGER, AnsiColor.YELLOW);
		LEVELS = Collections.unmodifiableMap(ansiLevels);
	}

	@Override
	protected String transform(ILoggingEvent event, String in) {
		List<String> options = getOptionList();
		List<AnsiElement> elements = new ArrayList<>();
		if (options != null) {
			for (String option : options) {
				String[] optionParts = option.split(",");
				for (String optionPart : optionParts) {
					AnsiElement element = ELEMENTS.get(optionPart.trim().toLowerCase(Locale.ROOT));
					if (element != null) {
						elements.add(element);
					}
				}
			}
		}
		if (elements.isEmpty()) {
			// Assume highlighting
			AnsiElement element = LEVELS.get(event.getLevel().toInteger());
			elements.add((element != null) ? element : AnsiColor.GREEN);
		}
		return toAnsiString(in, elements.toArray(new AnsiElement[0]));
	}

	protected String toAnsiString(String in, AnsiElement element) {
		return AnsiOutput.toString(element, in);
	}

	protected String toAnsiString(String in, AnsiElement... elements) {
		Object[] ansiParams = new Object[elements.length + 1];
		System.arraycopy(elements, 0, ansiParams, 0, elements.length);
		ansiParams[elements.length] = in;
		return AnsiOutput.toString(ansiParams);
	}

	static String getName(AnsiElement element) {
		return ELEMENTS.entrySet()
			.stream()
			.filter((entry) -> entry.getValue().equals(element))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElseThrow();
	}

}
