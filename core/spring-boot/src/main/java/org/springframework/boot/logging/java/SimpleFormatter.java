/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.java;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.springframework.boot.logging.LoggingSystemProperty;

/**
 * Simple 'Java Logging' {@link Formatter}.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public class SimpleFormatter extends Formatter {

	private static final String DEFAULT_FORMAT = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL] - %8$s %4$s [%7$s] --- %3$s: %5$s%6$s%n";

	private final String format = getOrUseDefault("LOG_FORMAT", DEFAULT_FORMAT);

	private final String pid = getOrUseDefault(LoggingSystemProperty.PID.getEnvironmentVariableName(), "????");

	@Override
	public String format(LogRecord record) {
		Date date = new Date(record.getMillis());
		String source = record.getLoggerName();
		String loggerName = record.getLoggerName();
		String level = record.getLevel().getLocalizedName();
		String message = formatMessage(record);
		String throwable = getThrowable(record);
		String thread = getThreadName();
		String pid = this.pid;
		return String.format(this.format, date, source, loggerName, level, message, throwable, thread, pid);
	}

	private String getThrowable(LogRecord record) {
		if (record.getThrown() == null) {
			return "";
		}
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		printWriter.println();
		record.getThrown().printStackTrace(printWriter);
		printWriter.close();
		return stringWriter.toString();
	}

	private String getThreadName() {
		String name = Thread.currentThread().getName();
		return (name != null) ? name : "";
	}

	private static String getOrUseDefault(String key, String defaultValue) {
		String value = null;
		try {
			value = System.getenv(key);
		}
		catch (Exception ex) {
			// ignore
		}
		if (value == null) {
			value = defaultValue;
		}
		return System.getProperty(key, value);
	}

}
