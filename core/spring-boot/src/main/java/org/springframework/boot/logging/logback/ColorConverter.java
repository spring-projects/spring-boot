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
import org.jspecify.annotations.Nullable;

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
		Arrays.stream(AnsiColor.values())
			.filter((color) -> color != AnsiColor.DEFAULT)
			.forEach((color) -> ansiElements.put(color.name().toLowerCase(Locale.ROOT), color));
		Arrays.stream(AnsiStyle.values())
			.forEach((style) -> ansiElements.put(style.name().toLowerCase(Locale.ROOT), style));
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

	private List<AnsiElement> stylings = Collections.emptyList();

	@Override
	public void start() {
		this.stylings = getStylings(getOptionList());
		super.start();
	}

	@Override
	protected String transform(ILoggingEvent event, String in) {
		if (this.stylings.isEmpty()) {
			// Assume highlighting
			AnsiElement element = LEVELS.get(event.getLevel().toInteger());
			return toAnsiString(in, (element != null) ? element : AnsiColor.GREEN);
		}
		if (this.stylings.size() == 1) {
			return toAnsiString(in, this.stylings.get(0));
		}
		return toAnsiString(in, this.stylings.toArray(new AnsiElement[0]));
	}

	private static List<AnsiElement> getStylings(@Nullable List<String> options) {
		if (options == null) {
			return Collections.emptyList();
		}
		List<AnsiElement> stylings = new ArrayList<>();
		for (String option : options) {
			for (String optionPart : option.split(",")) {
				AnsiElement element = ELEMENTS.get(optionPart.trim().toLowerCase(Locale.ROOT));
				if (element != null) {
					stylings.add(element);
				}
			}
		}
		return stylings;
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
