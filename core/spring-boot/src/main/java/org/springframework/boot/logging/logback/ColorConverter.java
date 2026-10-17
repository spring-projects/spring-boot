/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.util.CollectionUtils;

/**
 * Logback {@link CompositeConverter} to color output using the {@link AnsiOutput} class.
 * One or more styling options can be provided to the converter, or if not specified color
 * will be picked based on the logging level. Supported options include foreground colors
 * (e.g. {@code red}, {@code bright_blue}), background colors (e.g. {@code bg_red},
 * {@code bg_bright_green}), and text styles (e.g. {@code bold}, {@code underline},
 * {@code reverse}).
 *
 * @author Phillip Webb
 * @author Jan Mohr
 * @since 1.0.0
 */
public class ColorConverter extends CompositeConverter<ILoggingEvent> {

	private static final Map<String, AnsiElement> ELEMENTS_BY_NAME;

	static {
		Map<String, AnsiElement> elements = new HashMap<>();
		Arrays.stream(AnsiColor.values())
			.filter((color) -> color != AnsiColor.DEFAULT)
			.forEach((color) -> elements.put(color.name().toLowerCase(Locale.ROOT), color));
		Arrays.stream(AnsiStyle.values())
			.forEach((style) -> elements.put(style.name().toLowerCase(Locale.ROOT), style));
		Arrays.stream(AnsiBackground.values())
			.filter((background) -> background != AnsiBackground.DEFAULT)
			.forEach((background) -> elements.put("bg_" + background.name().toLowerCase(Locale.ROOT), background));
		ELEMENTS_BY_NAME = Collections.unmodifiableMap(elements);
	}

	private static final Map<Integer, AnsiElement> ELEMENTS_BY_LEVEL;

	static {
		Map<Integer, AnsiElement> elements = new HashMap<>();
		elements.put(Level.ERROR_INTEGER, AnsiColor.RED);
		elements.put(Level.WARN_INTEGER, AnsiColor.YELLOW);
		ELEMENTS_BY_LEVEL = Collections.unmodifiableMap(elements);
	}

	private static final AnsiElement[] NO_ELEMENTS = {};

	private AnsiElement[] elements = NO_ELEMENTS;

	@Override
	public void start() {
		List<String> options = getOptionList();
		if (!CollectionUtils.isEmpty(options)) {
			List<AnsiElement> elements = new ArrayList<>();
			for (String option : options) {
				for (String optionPart : option.split(",")) {
					AnsiElement element = ELEMENTS_BY_NAME.get(optionPart.trim().toLowerCase(Locale.ROOT));
					if (element != null) {
						elements.add(element);
					}
				}
			}
			this.elements = elements.toArray(AnsiElement[]::new);
		}
		super.start();
	}

	@Override
	protected String transform(ILoggingEvent event, String in) {
		if (this.elements.length == 0) {
			// Assume highlighting
			AnsiElement element = ELEMENTS_BY_LEVEL.get(event.getLevel().toInteger());
			return toAnsiString(in, (element != null) ? element : AnsiColor.GREEN);
		}
		return toAnsiString(in, this.elements);
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
		return ELEMENTS_BY_NAME.entrySet()
			.stream()
			.filter((entry) -> entry.getValue().equals(element))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElseThrow();
	}

}
