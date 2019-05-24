/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.File;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for default Logback configuration provided by {@code base.xml}.
 *
 * @author Andy Wilkinson
 */
class LogbackConfigurationTests {

	@Test
	void consolePatternCanBeOverridden() throws JoranException {
		JoranConfigurator configurator = new JoranConfigurator();
		LoggerContext context = new LoggerContext();
		configurator.setContext(context);
		configurator.doConfigure(new File("src/test/resources/custom-console-log-pattern.xml"));
		Appender<ILoggingEvent> appender = context.getLogger("ROOT").getAppender("CONSOLE");
		assertThat(appender).isInstanceOf(ConsoleAppender.class);
		Encoder<?> encoder = ((ConsoleAppender<?>) appender).getEncoder();
		assertThat(encoder).isInstanceOf(PatternLayoutEncoder.class);
		assertThat(((PatternLayoutEncoder) encoder).getPattern()).isEqualTo("foo");
	}

	@Test
	void filePatternCanBeOverridden() throws JoranException {
		JoranConfigurator configurator = new JoranConfigurator();
		LoggerContext context = new LoggerContext();
		configurator.setContext(context);
		configurator.doConfigure(new File("src/test/resources/custom-file-log-pattern.xml"));
		Appender<ILoggingEvent> appender = context.getLogger("ROOT").getAppender("FILE");
		assertThat(appender).isInstanceOf(FileAppender.class);
		Encoder<?> encoder = ((FileAppender<?>) appender).getEncoder();
		assertThat(encoder).isInstanceOf(PatternLayoutEncoder.class);
		assertThat(((PatternLayoutEncoder) encoder).getPattern()).isEqualTo("bar");
	}

}
