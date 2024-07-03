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
 * {@link LoggingSystemProperty#LOGGED_APPLICATION_GROUP APPLICATION_GROUP} into a value
 * suitable for logging. Similar to Logback's {@link PropertyConverter} but a non-existent
 * property is logged as an empty string rather than {@code null}.
 *
 * @author Jakob Wanger
 * @since 3.4.0
 */
public class ApplicationGroupConverter extends ClassicConverter {

	@Override
	public String convert(ILoggingEvent event) {
		String applicationGroup = event.getLoggerContextVO()
			.getPropertyMap()
			.get(LoggingSystemProperty.LOGGED_APPLICATION_GROUP.getEnvironmentVariableName());
		if (applicationGroup == null) {
			applicationGroup = System
				.getProperty(LoggingSystemProperty.LOGGED_APPLICATION_GROUP.getEnvironmentVariableName());
			if (applicationGroup == null) {
				applicationGroup = "";
			}
		}
		return applicationGroup;
	}

}
