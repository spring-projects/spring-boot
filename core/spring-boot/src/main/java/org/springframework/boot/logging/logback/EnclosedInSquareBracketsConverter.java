/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * Logback {@link CompositeConverter} used to help format optional values that should be
 * shown enclosed in square brackets.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
public class EnclosedInSquareBracketsConverter extends CompositeConverter<ILoggingEvent> {

	@Override
	protected String transform(ILoggingEvent event, @Nullable String in) {
		in = (!StringUtils.hasLength(in)) ? resolveFromFirstOption(event) : in;
		return (!StringUtils.hasLength(in)) ? "" : "[%s] ".formatted(in);
	}

	private @Nullable String resolveFromFirstOption(ILoggingEvent event) {
		String name = getFirstOption();
		if (name == null) {
			return null;
		}
		String value = event.getLoggerContextVO().getPropertyMap().get(name);
		return (value != null) ? value : System.getProperty(name);
	}

}
