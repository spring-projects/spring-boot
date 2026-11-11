/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.structuredlogging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;

public class CustomStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

	private final @Nullable Long pid;

	private final ThrowableProxyConverter throwableProxyConverter;

	public CustomStructuredLogFormatter(Environment environment, ThrowableProxyConverter throwableProxyConverter) {
		this.pid = environment.getProperty("spring.application.pid", Long.class);
		this.throwableProxyConverter = throwableProxyConverter;
	}

	@Override
	public String format(ILoggingEvent event) {
		StringBuilder result = new StringBuilder();
		result.append("epoch=").append(event.getInstant().toEpochMilli());
		if (this.pid != null) {
			result.append(" pid=").append(this.pid);
		}
		result.append(" msg=\"").append(event.getFormattedMessage()).append('"');
		IThrowableProxy throwable = event.getThrowableProxy();
		if (throwable != null) {
			result.append(" error=\"").append(this.throwableProxyConverter.convert(event)).append('"');
		}
		result.append('\n');
		return result.toString();
	}

}
