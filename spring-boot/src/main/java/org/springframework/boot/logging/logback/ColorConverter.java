/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
		Map<String, AnsiElement> elements = new HashMap<String, AnsiElement>();
		elements.put("faint", AnsiStyle.FAINT);
		elements.put("red", AnsiColor.RED);
		elements.put("green", AnsiColor.GREEN);
		elements.put("yellow", AnsiColor.YELLOW);
		elements.put("blue", AnsiColor.BLUE);
		elements.put("magenta", AnsiColor.MAGENTA);
		elements.put("cyan", AnsiColor.CYAN);
		ELEMENTS = Collections.unmodifiableMap(elements);
	}

	private static final Map<Integer, AnsiElement> LEVELS;

	static {
		Map<Integer, AnsiElement> levels = new HashMap<Integer, AnsiElement>();
		levels.put(Level.ERROR_INTEGER, AnsiColor.RED);
		levels.put(Level.WARN_INTEGER, AnsiColor.YELLOW);
		LEVELS = Collections.unmodifiableMap(levels);
	}

	@Override
	protected String transform(ILoggingEvent event, String in) {
		AnsiElement element = ELEMENTS.get(getFirstOption());
		if (element == null) {
			// Assume highlighting
			element = LEVELS.get(event.getLevel().toInteger());
			element = (element == null ? AnsiColor.GREEN : element);
		}
		return toAnsiString(in, element);
	}

	protected String toAnsiString(String in, AnsiElement element) {
		return AnsiOutput.toString(element, in);
	}

}
