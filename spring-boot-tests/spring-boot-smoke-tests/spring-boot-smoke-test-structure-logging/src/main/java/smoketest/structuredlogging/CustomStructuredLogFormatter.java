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

package smoketest.structuredlogging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;

public class CustomStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

	private final Long pid;

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
