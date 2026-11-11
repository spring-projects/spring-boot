/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.error;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.error.ErrorAttributeOptions.Include;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ErrorAttributeOptions}.
 *
 * @author Wanderlei Souza
 * @author Stephane Nicoll
 */
class ErrorAttributesOptionsTests {

	@Test
	void includingFromEmptyAttributesReturnAddedEntry() {
		ErrorAttributeOptions options = ErrorAttributeOptions.of(EnumSet.noneOf(Include.class));
		assertThat(options.including(Include.EXCEPTION).getIncludes()).containsOnly(Include.EXCEPTION);
	}

	@Test
	void includingFromMatchingAttributesDoesNotModifyOptions() {
		ErrorAttributeOptions options = ErrorAttributeOptions.of(EnumSet.of(Include.EXCEPTION, Include.STACK_TRACE));
		assertThat(options.including(Include.EXCEPTION).getIncludes()).containsOnly(Include.EXCEPTION,
				Include.STACK_TRACE);
	}

	@Test
	void excludingFromEmptyAttributesReturnEmptyList() {
		ErrorAttributeOptions options = ErrorAttributeOptions.of(EnumSet.noneOf(Include.class));
		assertThat(options.excluding(Include.EXCEPTION).getIncludes()).isEmpty();
	}

	@Test
	void excludingFromMatchingAttributesRemoveMatch() {
		ErrorAttributeOptions options = ErrorAttributeOptions.of(EnumSet.of(Include.EXCEPTION, Include.STACK_TRACE));
		assertThat(options.excluding(Include.EXCEPTION).getIncludes()).containsOnly(Include.STACK_TRACE);
	}

}
