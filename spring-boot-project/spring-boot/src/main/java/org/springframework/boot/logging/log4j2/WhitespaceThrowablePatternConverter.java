/*
 * Copyright 2012-2019 the original author or authors.
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
public final class WhitespaceThrowablePatternConverter extends ThrowablePatternConverter {

	/**
     * Constructs a new WhitespaceThrowablePatternConverter with the specified configuration and options.
     *
     * @param configuration the configuration to be used
     * @param options the options to be applied
     */
    private WhitespaceThrowablePatternConverter(Configuration configuration, String[] options) {
		super("WhitespaceThrowable", "throwable", options, configuration);
	}

	/**
     * Formats the log event by appending the separator from the options, the log event message, and the separator again if the log event has a thrown exception.
     * 
     * @param event the log event to be formatted
     * @param buffer the StringBuilder to append the formatted log event to
     */
    @Override
	public void format(LogEvent event, StringBuilder buffer) {
		if (event.getThrown() != null) {
			buffer.append(this.options.getSeparator());
			super.format(event, buffer);
			buffer.append(this.options.getSeparator());
		}
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
