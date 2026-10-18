/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SanitizableData}.
 *
 * @author Phillip Webb
 */
class SanitizableDataTests {

	private final PropertySource<?> propertySource = new MapPropertySource("test", Map.of("key", "value"));

	@Test
	void getPropertySourceReturnsPropertySource() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		assertThat(data.getPropertySource()).isSameAs(this.propertySource);
	}

	@Test
	void getKeyReturnsKey() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		assertThat(data.getKey()).isEqualTo("key");
	}

	@Test
	void getValueReturnsValue() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		assertThat(data.getValue()).isEqualTo("value");
	}

	@Test
	void withSanitizedValueReturnsNewInstanceWithSanitizedValue() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		SanitizableData sanitized = data.withSanitizedValue();
		assertThat(data.getValue()).isEqualTo("value");
		assertThat(sanitized.getValue()).isEqualTo("******");
	}

	@Test
	void withValueReturnsNewInstanceWithNewValue() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		SanitizableData sanitized = data.withValue("eulav");
		assertThat(data.getValue()).isEqualTo("value");
		assertThat(sanitized.getValue()).isEqualTo("eulav");
	}

	@Test
	void getLowerCaseKeyWhenNonEnglishLocaleReturnsFixedLowercaseKey() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr"));
		try {
			SanitizableData data = new SanitizableData(this.propertySource, "SPRING.I", "value");
			assertThat(data.getLowerCaseKey()).isEqualTo("spring.i");
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

}
