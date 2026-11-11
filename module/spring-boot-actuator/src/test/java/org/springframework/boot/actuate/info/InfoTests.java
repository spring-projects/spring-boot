/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link Info}.
 *
 * @author Stephane Nicoll
 */
class InfoTests {

	@Test
	void infoIsImmutable() {
		Info info = new Info.Builder().withDetail("foo", "bar").build();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(info.getDetails()::clear);
	}

	@Test
	void infoTakesCopyOfMap() {
		Info.Builder builder = new Info.Builder();
		builder.withDetail("foo", "bar");
		Info build = builder.build();
		builder.withDetail("biz", "bar");
		assertThat(build.getDetails()).containsOnly(entry("foo", "bar"));
	}

}
