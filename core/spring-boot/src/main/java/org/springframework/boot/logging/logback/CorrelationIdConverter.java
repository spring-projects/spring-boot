/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.Map;

import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.DynamicConverter;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.logging.CorrelationIdFormatter;
import org.springframework.core.env.Environment;

/**
 * Logback {@link DynamicConverter} to convert a {@link CorrelationIdFormatter} pattern
 * into formatted output using data from the {@link ILoggingEvent#getMDCPropertyMap() MDC}
 * and {@link Environment}.
 *
 * @author Phillip Webb
 * @since 3.2.0
 * @see MDCConverter
 */
public class CorrelationIdConverter extends DynamicConverter<ILoggingEvent> {

	private @Nullable CorrelationIdFormatter formatter;

	@Override
	public void start() {
		this.formatter = CorrelationIdFormatter.of(getOptionList());
		super.start();
	}

	@Override
	public void stop() {
		this.formatter = null;
		super.stop();
	}

	@Override
	public String convert(ILoggingEvent event) {
		if (this.formatter == null) {
			return "";
		}
		Map<String, String> mdc = event.getMDCPropertyMap();
		return this.formatter.format(mdc::get);
	}

}
