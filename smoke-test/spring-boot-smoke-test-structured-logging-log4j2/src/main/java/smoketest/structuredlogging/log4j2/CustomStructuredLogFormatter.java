/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.structuredlogging.log4j2;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.core.LogEvent;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;

public class CustomStructuredLogFormatter implements StructuredLogFormatter<LogEvent> {

	private final @Nullable Long pid;

	public CustomStructuredLogFormatter(Environment environment) {
		this.pid = environment.getProperty("spring.application.pid", Long.class);
	}

	@Override
	public String format(LogEvent event) {
		StringBuilder result = new StringBuilder();
		result.append("epoch=").append(event.getInstant().getEpochMillisecond());
		if (this.pid != null) {
			result.append(" pid=").append(this.pid);
		}
		result.append(" msg=\"").append(event.getMessage().getFormattedMessage()).append('"');
		Throwable throwable = event.getThrown();
		if (throwable != null) {
			StringWriter stackTrace = new StringWriter();
			throwable.printStackTrace(new PrintWriter(stackTrace));
			result.append(" error=\"").append(stackTrace).append('"');
		}
		result.append('\n');
		return result.toString();
	}

}
