/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConventionUtils}.
 *
 * @author Stephane Nicoll
 */
class ConventionUtilsTests {

	@Test
	void toDashedCaseCamelCase() {
		assertThat(toDashedCase("simpleCamelCase")).isEqualTo("simple-camel-case");
	}

	@Test
	void toDashedCaseUpperCamelCaseSuffix() {
		assertThat(toDashedCase("myDLQ")).isEqualTo("my-d-l-q");
	}

	@Test
	void toDashedCaseUpperCamelCaseMiddle() {
		assertThat(toDashedCase("someDLQKey")).isEqualTo("some-d-l-q-key");
	}

	@Test
	void toDashedCaseWordsUnderscore() {
		assertThat(toDashedCase("Word_With_underscore")).isEqualTo("word-with-underscore");
	}

	@Test
	void toDashedCaseWordsSeveralUnderscores() {
		assertThat(toDashedCase("Word___With__underscore")).isEqualTo("word---with--underscore");
	}

	@Test
	void toDashedCaseLowerCaseUnderscore() {
		assertThat(toDashedCase("lower_underscore")).isEqualTo("lower-underscore");
	}

	@Test
	void toDashedCaseUpperUnderscoreSuffix() {
		assertThat(toDashedCase("my_DLQ")).isEqualTo("my-d-l-q");
	}

	@Test
	void toDashedCaseUpperUnderscoreMiddle() {
		assertThat(toDashedCase("some_DLQ_key")).isEqualTo("some-d-l-q-key");
	}

	@Test
	void toDashedCaseMultipleUnderscores() {
		assertThat(toDashedCase("super___crazy")).isEqualTo("super---crazy");
	}

	@Test
	void toDashedCaseLowercase() {
		assertThat(toDashedCase("lowercase")).isEqualTo("lowercase");
	}

	private String toDashedCase(String name) {
		return ConventionUtils.toDashedCase(name);
	}

}
