/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;

/**
 * {@link ThrowablePatternConverter} that adds some additional whitespace around the stack
 * trace.
 *
 * @author Vladimir Tsanev
 */
@Plugin(name = "WhitespaceThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "wEx", "wThrowable", "wException" })
public class WhitespaceThrowablePatternConverter extends ThrowablePatternConverter {
	private WhitespaceThrowablePatternConverter(final String[] options) {
		super("WhitespaceThrowable", "throwable", options);
	}

	/**
	 * Gets an instance of the class.
	 *
	 * @param options pattern options, may be null. If first element is "short", only the
	 * first line of the throwable will be formatted.
	 * @return instance of class.
	 */
	public static WhitespaceThrowablePatternConverter newInstance(final String[] options) {
		return new WhitespaceThrowablePatternConverter(options);
	}

	@Override
	public void format(LogEvent event, StringBuilder buffer) {
		if (event.getThrown() != null) {
			buffer.append(options.getSeparator());
			super.format(event, buffer);
			buffer.append(options.getSeparator());
		}
	}
}
