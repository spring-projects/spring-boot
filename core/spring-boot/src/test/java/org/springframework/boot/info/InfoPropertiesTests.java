/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.info;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link InfoProperties}.
 *
 * @author Stephane Nicoll
 */
class InfoPropertiesTests {

	@Test
	void inputIsImmutable() {
		Properties p = new Properties();
		p.put("foo", "bar");
		InfoProperties infoProperties = new InfoProperties(p);
		assertThat(infoProperties.get("foo")).isEqualTo("bar");
		p.remove("foo");
		assertThat(infoProperties.get("foo")).isEqualTo("bar");
	}

	@Test
	void iterator() {
		Properties p = new Properties();
		p.put("one", "first");
		p.put("two", "second");
		InfoProperties infoProperties = new InfoProperties(p);
		Properties copy = new Properties();
		for (InfoProperties.Entry entry : infoProperties) {
			copy.put(entry.getKey(), entry.getValue());
		}
		assertThat(p).isEqualTo(copy);
	}

	@Test
	void removeNotSupported() {
		Properties p = new Properties();
		p.put("foo", "bar");
		InfoProperties infoProperties = new InfoProperties(p);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(infoProperties.iterator()::remove);
	}

	@Test
	void toPropertySources() {
		Properties p = new Properties();
		p.put("one", "first");
		p.put("two", "second");
		InfoProperties infoProperties = new MyInfoProperties(p);
		PropertySource<?> source = infoProperties.toPropertySource();
		assertThat(source.getProperty("one")).isEqualTo("first");
		assertThat(source.getProperty("two")).isEqualTo("second");
		assertThat(source.getName()).isEqualTo("MyInfoProperties");
	}

	static class MyInfoProperties extends InfoProperties {

		MyInfoProperties(Properties entries) {
			super(entries);
		}

	}

}
