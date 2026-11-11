/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.util.Assert;

/**
 * Functions to extract items from {@link ILoggingEvent}.
 *
 * @author Phillip Webb
 */
class Extractor {

	private final @Nullable StackTracePrinter stackTracePrinter;

	private final ThrowableProxyConverter throwableProxyConverter;

	Extractor(@Nullable StackTracePrinter stackTracePrinter, ThrowableProxyConverter throwableProxyConverter) {
		this.stackTracePrinter = stackTracePrinter;
		this.throwableProxyConverter = throwableProxyConverter;
	}

	String messageAndStackTrace(ILoggingEvent event) {
		return event.getFormattedMessage() + "\n\n" + stackTrace(event);
	}

	String stackTrace(ILoggingEvent event) {
		if (this.stackTracePrinter != null) {
			IThrowableProxy throwableProxy = event.getThrowableProxy();
			Assert.state(throwableProxy instanceof ThrowableProxy,
					"Instance must be a ThrowableProxy in order to print exception");
			Throwable throwable = ((ThrowableProxy) throwableProxy).getThrowable();
			return this.stackTracePrinter.printStackTraceToString(throwable);
		}
		return this.throwableProxyConverter.convert(event);
	}

}
