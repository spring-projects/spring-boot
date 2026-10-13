/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.MdcPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.logging.CorrelationIdFormatter;
import org.springframework.util.ObjectUtils;

/**
 * Log4j2 {@link LogEventPatternConverter} to convert a {@link CorrelationIdFormatter}
 * pattern into formatted output using data from the {@link LogEvent#getContextData()
 * MDC}.
 *
 * @author Phillip Webb
 * @since 3.2.0
 * @see MdcPatternConverter
 */
@Plugin(name = "CorrelationIdConverter", category = PatternConverter.CATEGORY)
@ConverterKeys("correlationId")
@PerformanceSensitive("allocation")
public final class CorrelationIdConverter extends LogEventPatternConverter {

	private final CorrelationIdFormatter formatter;

	private CorrelationIdConverter(CorrelationIdFormatter formatter) {
		super("correlationId{%s}".formatted(formatter), "mdc");
		this.formatter = formatter;
	}

	@Override
	public void format(LogEvent event, StringBuilder toAppendTo) {
		ReadOnlyStringMap contextData = event.getContextData();
		this.formatter.formatTo(contextData::getValue, toAppendTo);
	}

	/**
	 * Factory method to create a new {@link CorrelationIdConverter}.
	 * @param options options, may be null or first element contains name of property to
	 * format.
	 * @return instance of PropertiesPatternConverter.
	 */
	public static CorrelationIdConverter newInstance(String @Nullable [] options) {
		String pattern = (!ObjectUtils.isEmpty(options)) ? options[0] : null;
		return new CorrelationIdConverter(CorrelationIdFormatter.of(pattern));
	}

}
