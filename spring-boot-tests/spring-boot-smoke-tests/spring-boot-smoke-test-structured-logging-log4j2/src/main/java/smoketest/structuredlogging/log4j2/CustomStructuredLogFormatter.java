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

package smoketest.structuredlogging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;

public class CustomStructuredLogFormatter implements StructuredLogFormatter<LogEvent> {

	private final Long pid;

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
		ThrowableProxy throwable = event.getThrownProxy();
		if (throwable != null) {
			result.append(" error=\"").append(throwable.getExtendedStackTraceAsString()).append('"');
		}
		result.append('\n');
		return result.toString();
	}

}
