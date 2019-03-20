/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Logback {@link CompositeConverter} colors output using the {@link AnsiOutput} class. A
 * single 'color' option can be provided to the converter, or if not specified color will
 * be picked based on the logging level.
 *
 * @author Phillip Webb
 */
public class ColorConverter extends CompositeConverter<ILoggingEvent> {

	private static final Map<String, AnsiElement> ELEMENTS;

	static {
		Map<String, AnsiElement> ansiElements = new HashMap<String, AnsiElement>();
		ansiElements.put("faint", AnsiStyle.FAINT);
		ansiElements.put("red", AnsiColor.RED);
		ansiElements.put("green", AnsiColor.GREEN);
		ansiElements.put("yellow", AnsiColor.YELLOW);
		ansiElements.put("blue", AnsiColor.BLUE);
		ansiElements.put("magenta", AnsiColor.MAGENTA);
		ansiElements.put("cyan", AnsiColor.CYAN);
		ELEMENTS = Collections.unmodifiableMap(ansiElements);
	}

	private static final Map<Integer, AnsiElement> LEVELS;

	static {
		Map<Integer, AnsiElement> ansiLevels = new HashMap<Integer, AnsiElement>();
		ansiLevels.put(Level.ERROR_INTEGER, AnsiColor.RED);
		ansiLevels.put(Level.WARN_INTEGER, AnsiColor.YELLOW);
		LEVELS = Collections.unmodifiableMap(ansiLevels);
	}

	@Override
	protected String transform(ILoggingEvent event, String in) {
		AnsiElement element = ELEMENTS.get(getFirstOption());
		if (element == null) {
			// Assume highlighting
			element = LEVELS.get(event.getLevel().toInteger());
			element = (element != null) ? element : AnsiColor.GREEN;
		}
		return toAnsiString(in, element);
	}

	protected String toAnsiString(String in, AnsiElement element) {
		return AnsiOutput.toString(element, in);
	}

}
