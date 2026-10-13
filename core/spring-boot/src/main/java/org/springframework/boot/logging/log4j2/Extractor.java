/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.core.LogEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.LoggingEvent;

import org.springframework.boot.logging.StackTracePrinter;

/**
 * Functions to extract items from {@link LoggingEvent}.
 *
 * @author Phillip Webb
 */
class Extractor {

	private final @Nullable StackTracePrinter stackTracePrinter;

	Extractor(@Nullable StackTracePrinter stackTracePrinter) {
		this.stackTracePrinter = stackTracePrinter;
	}

	String messageAndStackTrace(LogEvent event) {
		return event.getMessage().getFormattedMessage() + "\n\n" + stackTrace(event);
	}

	@Nullable String stackTrace(LogEvent event) {
		return stackTrace(event.getThrown());
	}

	@Nullable String stackTrace(@Nullable Throwable throwable) {
		if (throwable == null) {
			return null;
		}
		if (this.stackTracePrinter != null) {
			return this.stackTracePrinter.printStackTraceToString(throwable);
		}
		return printStackTrace(throwable);
	}

	private static String printStackTrace(Throwable throwable) {
		StringWriter stringWriter = new StringWriter();
		throwable.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString();
	}

}
