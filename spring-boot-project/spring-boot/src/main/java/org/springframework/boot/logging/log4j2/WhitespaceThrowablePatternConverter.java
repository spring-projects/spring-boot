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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.ExtendedThrowablePatternConverter;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;

/**
 * {@link ThrowablePatternConverter} that adds some additional whitespace around the stack
 * trace.
 *
 * @author Vladimir Tsanev
 * @since 1.3.0
 */
@Plugin(name = "WhitespaceThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "wEx", "wThrowable", "wException" })
public final class WhitespaceThrowablePatternConverter extends LogEventPatternConverter {

	private final LogEventPatternConverter delegate;

	private final String separator;

	private WhitespaceThrowablePatternConverter(Configuration configuration, String[] options) {
		super("WhitespaceThrowable", "throwable");
		this.delegate = ExtendedThrowablePatternConverter.newInstance(configuration, options);
		this.separator = readSeparatorOption(options);
	}

	static String readSeparatorOption(String[] options) {
		if (options != null) {
			for (String option : options) {
				if (option != null && option.startsWith("separator(") && option.endsWith(")")) {
					return option.substring("separator(".length(), option.length() - 1);
				}
			}
		}
		return System.lineSeparator();
	}

	@Override
	public void format(LogEvent event, StringBuilder buffer) {
		if (event.getThrown() != null) {
			buffer.append(this.separator);
			this.delegate.format(event, buffer);
			buffer.append(this.separator);
		}
	}

	@Override
	public boolean handlesThrowable() {
		return true;
	}

	/**
	 * Creates a new instance of the class. Required by Log4J2.
	 * @param configuration current configuration
	 * @param options pattern options, may be null. If first element is "short", only the
	 * first line of the throwable will be formatted.
	 * @return a new {@code WhitespaceThrowablePatternConverter}
	 */
	public static WhitespaceThrowablePatternConverter newInstance(Configuration configuration, String[] options) {
		return new WhitespaceThrowablePatternConverter(configuration, options);
	}

}
