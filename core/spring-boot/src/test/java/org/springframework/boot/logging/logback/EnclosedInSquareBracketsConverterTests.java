/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.Collections;
import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.jspecify.annotations.Nullable;
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

	private void withLoggedApplicationName(String name, @Nullable String value, Runnable action) {
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
