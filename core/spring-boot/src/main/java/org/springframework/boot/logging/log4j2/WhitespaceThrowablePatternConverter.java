/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.core.pattern.VariablesNotEmptyReplacementConverter;
import org.jspecify.annotations.Nullable;

/**
 * {@link ThrowablePatternConverter} that adds some additional whitespace around the stack
 * trace.
 *
 * @author Vladimir Tsanev
 * @since 1.3.0
 */
@Plugin(name = "WhitespaceThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "wEx", "wThrowable", "wException" })
public final class WhitespaceThrowablePatternConverter {

	private WhitespaceThrowablePatternConverter() {
	}

	public static LogEventPatternConverter newInstance(Configuration configuration, String @Nullable [] options) {
		final String nested = (options != null && options.length > 0) ? "{" + String.join("}{", options) + "}" : "";
		final String pattern = "%notEmpty{%n%ex" + nested + "%n}";
		return VariablesNotEmptyReplacementConverter.newInstance(configuration, new String[] { pattern });
	}

}
