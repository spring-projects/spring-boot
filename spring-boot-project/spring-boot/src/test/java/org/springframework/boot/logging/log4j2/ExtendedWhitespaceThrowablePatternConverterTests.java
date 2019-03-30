/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExtendedWhitespaceThrowablePatternConverter}.
 *
 * @author Vladimir Tsanev
 * @author Phillip Webb
 */
public class ExtendedWhitespaceThrowablePatternConverterTests {

	private final ThrowablePatternConverter converter = ExtendedWhitespaceThrowablePatternConverter
			.newInstance(new DefaultConfiguration(), new String[] {});

	@Test
	public void noStackTrace() {
		LogEvent event = Log4jLogEvent.newBuilder().build();
		StringBuilder builder = new StringBuilder();
		this.converter.format(event, builder);
		assertThat(builder.toString()).isEqualTo("");
	}

	@Test
	public void withStackTrace() {
		LogEvent event = Log4jLogEvent.newBuilder().setThrown(new Exception()).build();
		StringBuilder builder = new StringBuilder();
		this.converter.format(event, builder);
		assertThat(builder).startsWith(System.lineSeparator())
				.endsWith(System.lineSeparator());
	}

}
