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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

import org.springframework.util.StringUtils;

/**
 * Logback {@link CompositeConverter} used to help format optional values that should be
 * shown enclosed in square brackets.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
public class EnclosedInSquareBracketsConverter extends CompositeConverter<ILoggingEvent> {

	@Override
	protected String transform(ILoggingEvent event, String in) {
		in = (!StringUtils.hasLength(in)) ? resolveFromFirstOption(event) : in;
		return (!StringUtils.hasLength(in)) ? "" : "[%s] ".formatted(in);
	}

	private String resolveFromFirstOption(ILoggingEvent event) {
		String name = getFirstOption();
		if (name == null) {
			return null;
		}
		String value = event.getLoggerContextVO().getPropertyMap().get(name);
		return (value != null) ? value : System.getProperty(name);
	}

}
