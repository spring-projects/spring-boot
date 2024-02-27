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

import java.util.Collections;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.LoggingSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationNameConverter}.
 *
 * @author Andy Wilkinson
 */
class ApplicationNameConverterTests {

	private final ApplicationNameConverter converter;

	private final LoggingEvent event = new LoggingEvent();

	ApplicationNameConverterTests() {
		this.converter = new ApplicationNameConverter();
		this.converter.setContext(new LoggerContext());
		this.event.setLoggerContextRemoteView(
				new LoggerContextVO("test", Collections.emptyMap(), System.currentTimeMillis()));
	}

	@Test
	void whenNoLoggedApplicationNameConvertReturnsEmptyString() {
		withLoggedApplicationName(null, () -> {
			this.converter.start();
			String converted = this.converter.convert(this.event);
			assertThat(converted).isEqualTo("");
		});
	}

	@Test
	void whenLoggedApplicationNameConvertReturnsIt() {
		withLoggedApplicationName("my-application", () -> {
			this.converter.start();
			String converted = this.converter.convert(this.event);
			assertThat(converted).isEqualTo("my-application");
		});
	}

	private void withLoggedApplicationName(String name, Runnable action) {
		if (name == null) {
			System.clearProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName());
		}
		else {
			System.setProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName(), name);
		}
		try {
			action.run();
		}
		finally {
			System.clearProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName());
		}
	}

}
