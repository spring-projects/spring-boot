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

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.pattern.PropertyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import org.springframework.boot.logging.LoggingSystemProperty;

/**
 * Logback {@link ClassicConverter} to convert the
 * {@link LoggingSystemProperty#APPLICATION_NAME APPLICATION_NAME} into a value suitable
 * for logging. Similar to Logback's {@link PropertyConverter} but a non-existent property
 * is logged as an empty string rather than {@code null}.
 *
 * @author Andy Wilkinson
 * @since 3.2.4
 */
public class ApplicationNameConverter extends ClassicConverter {

	@Override
	public String convert(ILoggingEvent event) {
		String applicationName = event.getLoggerContextVO()
			.getPropertyMap()
			.get(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName());
		if (applicationName == null) {
			applicationName = System.getProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName());
			if (applicationName == null) {
				applicationName = "";
			}
		}
		return applicationName;
	}

}
