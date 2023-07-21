/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Map;

import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.DynamicConverter;

import org.springframework.boot.logging.CorrelationIdFormatter;
import org.springframework.core.env.Environment;

/**
 * Logback {@link DynamicConverter} to convert a {@link CorrelationIdFormatter} pattern
 * into formatted output using data from the {@link ILoggingEvent#getMDCPropertyMap() MDC}
 * and {@link Environment}.
 *
 * @author Phillip Webb
 * @since 3.2.0
 * @see MDCConverter
 */
public class CorrelationIdConverter extends DynamicConverter<ILoggingEvent> {

	private CorrelationIdFormatter formatter;

	@Override
	public void start() {
		this.formatter = CorrelationIdFormatter.of(getOptionList());
		super.start();
	}

	@Override
	public void stop() {
		this.formatter = null;
		super.stop();
	}

	@Override
	public String convert(ILoggingEvent event) {
		if (this.formatter == null) {
			return "";
		}
		Map<String, String> mdc = event.getMDCPropertyMap();
		return this.formatter.format(mdc::get);
	}

}
