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

package org.springframework.boot.logging.log4j2;

import java.util.Map;

import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CorrelationIdConverter}.
 *
 * @author Phillip Webb
 */
class CorrelationIdConverterTests {

	private CorrelationIdConverter converter = CorrelationIdConverter.newInstance(null);

	private final LogEvent event = new TestLogEvent();

	@Test
	void defaultPattern() {
		StringBuilder result = new StringBuilder();
		this.converter.format(this.event, result);
		assertThat(result).hasToString("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void customPattern() {
		this.converter = CorrelationIdConverter.newInstance(new String[] { "traceId(0),spanId(0)" });
		StringBuilder result = new StringBuilder();
		this.converter.format(this.event, result);
		assertThat(result).hasToString("[01234567890123456789012345678901-0123456789012345] ");
	}

	static class TestLogEvent extends AbstractLogEvent {

		@Override
		public ReadOnlyStringMap getContextData() {
			return new JdkMapAdapterStringMap(
					Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"), true);
		}

	}

}
