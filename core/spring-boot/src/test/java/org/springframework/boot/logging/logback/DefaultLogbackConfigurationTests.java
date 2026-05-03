/*
 * Copyright 2012-present the original author or authors.
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

import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;

import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link DefaultLogbackConfiguration}
 *
 * @author Phillip Webb
 */
class DefaultLogbackConfigurationTests {

	private final LoggerContext loggerContext = new LoggerContext();

	private final LogbackConfigurator logbackConfigurator = new LogbackConfigurator(this.loggerContext);

	@Test
	void defaultLogbackXmlContainsConsoleLogPattern() throws Exception {
		assertThatDefaultXmlContains("CONSOLE_LOG_PATTERN", DefaultLogbackConfiguration.CONSOLE_LOG_PATTERN);
	}

	@Test
	void defaultLogbackXmlContainsFileLogPattern() throws Exception {
		assertThatDefaultXmlContains("FILE_LOG_PATTERN", DefaultLogbackConfiguration.FILE_LOG_PATTERN);
	}

	@Test
	void consoleLogCharsetShouldUseConsoleCharsetIfConsoleAvailable() {
		DefaultLogbackConfiguration logbackConfiguration = spy(new DefaultLogbackConfiguration(null));
		Console console = mock(Console.class);
		given(console.charset()).willReturn(StandardCharsets.UTF_16);
		given(logbackConfiguration.getConsole()).willReturn(console);
		logbackConfiguration.apply(this.logbackConfigurator);
		assertThat(this.loggerContext.getProperty("CONSOLE_LOG_CHARSET")).isEqualTo(StandardCharsets.UTF_16.name());
	}

	@Test
	void consoleLogCharsetShouldDefaultToUtf8WhenConsoleIsNull() {
		DefaultLogbackConfiguration logbackConfiguration = spy(new DefaultLogbackConfiguration(null));
		given(logbackConfiguration.getConsole()).willReturn(null);
		logbackConfiguration.apply(this.logbackConfigurator);
		assertThat(this.loggerContext.getProperty("CONSOLE_LOG_CHARSET")).isEqualTo(StandardCharsets.UTF_8.name());
	}

	@Test
	void consoleLogCharsetShouldUseSystemPropertyIfSet() {
		withSystemProperty("CONSOLE_LOG_CHARSET", StandardCharsets.US_ASCII.name(), () -> {
			new DefaultLogbackConfiguration(null).apply(this.logbackConfigurator);
			assertThat(this.loggerContext.getProperty("CONSOLE_LOG_CHARSET"))
				.isEqualTo(StandardCharsets.US_ASCII.name());
		});
	}

	@Test
	void fileLogCharsetShouldUseSystemPropertyIfSet() {
		withSystemProperty("FILE_LOG_CHARSET", StandardCharsets.ISO_8859_1.name(), () -> {
			new DefaultLogbackConfiguration(null).apply(this.logbackConfigurator);
			assertThat(this.loggerContext.getProperty("FILE_LOG_CHARSET"))
				.isEqualTo(StandardCharsets.ISO_8859_1.name());
		});
	}

	@Test
	void fileLogCharsetShouldDefaultToUtf8() {
		new DefaultLogbackConfiguration(null).apply(this.logbackConfigurator);
		assertThat(this.loggerContext.getProperty("FILE_LOG_CHARSET")).isEqualTo(StandardCharsets.UTF_8.name());
	}

	private void assertThatDefaultXmlContains(String name, String value) throws Exception {
		String expected = "<property name=\"%s\" value=\"%s\"/>".formatted(name, value);
		assertThat(defaultXmlContent()).contains(expected);
	}

	private String defaultXmlContent() throws IOException {
		return StreamUtils.copyToString(getClass().getResourceAsStream("defaults.xml"), StandardCharsets.UTF_8);
	}

	private static void withSystemProperty(String name, String value, Runnable action) {
		String previous = System.getProperty(name);
		try {
			System.setProperty(name, value);
			action.run();
		}
		finally {
			if (previous != null) {
				System.setProperty(name, previous);
			}
			else {
				System.clearProperty(name);
			}
		}

	}

}
