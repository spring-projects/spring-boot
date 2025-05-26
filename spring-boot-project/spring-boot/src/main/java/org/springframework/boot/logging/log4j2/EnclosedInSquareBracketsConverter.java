/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;

/**
 * Log4j2 {@link LogEventPatternConverter} used to help format optional values that should
 * be shown enclosed in square brackets.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
@Plugin(name = "enclosedInSquareBrackets", category = PatternConverter.CATEGORY)
@ConverterKeys("esb")
public final class EnclosedInSquareBracketsConverter extends LogEventPatternConverter {

	private final List<PatternFormatter> formatters;

	private EnclosedInSquareBracketsConverter(List<PatternFormatter> formatters) {
		super("enclosedInSquareBrackets", null);
		this.formatters = formatters;
	}

	@Override
	public void format(LogEvent event, StringBuilder toAppendTo) {
		StringBuilder buf = new StringBuilder();
		for (PatternFormatter formatter : this.formatters) {
			formatter.format(event, buf);
		}
		if (buf.isEmpty()) {
			return;
		}
		toAppendTo.append("[");
		toAppendTo.append(buf);
		toAppendTo.append("] ");
	}

	/**
	 * Creates a new instance of the class. Required by Log4J2.
	 * @param config the configuration
	 * @param options the options
	 * @return a new instance, or {@code null} if the options are invalid
	 */
	public static EnclosedInSquareBracketsConverter newInstance(Configuration config, String[] options) {
		if (options.length < 1) {
			LOGGER.error("Incorrect number of options on style. Expected at least 1, received {}", options.length);
			return null;
		}
		PatternParser parser = PatternLayout.createPatternParser(config);
		List<PatternFormatter> formatters = parser.parse(options[0]);
		return new EnclosedInSquareBracketsConverter(formatters);
	}

}
