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
import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnclosedInSquareBracketsConverter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class EnclosedInSquareBracketsConverterTests {

	private final EnclosedInSquareBracketsConverter converter;

	private final LoggingEvent event = new LoggingEvent();

	EnclosedInSquareBracketsConverterTests() {
		this.converter = new EnclosedInSquareBracketsConverter();
		this.converter.setContext(new LoggerContext());
		this.event.setLoggerContextRemoteView(
				new LoggerContextVO("test", Collections.emptyMap(), System.currentTimeMillis()));
	}

	@Test
	void transformWhenNull() {
		assertThat(this.converter.transform(this.event, null)).isEqualTo("");
	}

	@Test
	void transformWhenEmpty() {
		assertThat(this.converter.transform(this.event, "")).isEqualTo("");
	}

	@Test
	void transformWhenName() {
		assertThat(this.converter.transform(this.event, "My Application")).isEqualTo("[My Application] ");
	}

	@Test
	void transformWhenEmptyFromFirstOption() {
		withLoggedApplicationName("spring", null, () -> {
			this.converter.setOptionList(List.of("spring"));
			this.converter.start();
			String converted = this.converter.convert(this.event);
			assertThat(converted).isEqualTo("");
		});
	}

	@Test
	void transformWhenNameFromFirstOption() {
		withLoggedApplicationName("spring", "boot", () -> {
			this.converter.setOptionList(List.of("spring"));
			this.converter.start();
			String converted = this.converter.convert(this.event);
			assertThat(converted).isEqualTo("[boot] ");
		});
	}

	private void withLoggedApplicationName(String name, String value, Runnable action) {
		if (value == null) {
			System.clearProperty(name);
		}
		else {
			System.setProperty(name, value);
		}
		try {
			action.run();
		}
		finally {
			System.clearProperty(name);
		}
	}

}
