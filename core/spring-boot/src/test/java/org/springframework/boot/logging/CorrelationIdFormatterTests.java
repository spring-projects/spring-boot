/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CorrelationIdFormatter}.
 *
 * @author Phillip Webb
 */
class CorrelationIdFormatterTests {

	@Test
	void formatWithDefaultSpecWhenHasBothParts() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		context.put("spanId", "0123456789012345");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void formatWithDefaultSpecWhenHasNoParts() {
		Map<String, String> context = new HashMap<>();
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[                                                 ] ");
	}

	@Test
	void formatWithDefaultSpecWhenHasOnlyFirstPart() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-                ] ");
	}

	@Test
	void formatWithDefaultSpecWhenHasOnlySecondPart() {
		Map<String, String> context = new HashMap<>();
		context.put("spanId", "0123456789012345");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[                                -0123456789012345] ");
	}

	@Test
	void formatWhenPartsAreShort() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "0123456789012345678901234567");
		context.put("spanId", "012345678901");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[0123456789012345678901234567    -012345678901    ] ");
	}

	@Test
	void formatWhenPartsAreLong() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901FFFF");
		context.put("spanId", "0123456789012345FFFF");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901FFFF-0123456789012345FFFF] ");
	}

	@Test
	void formatWithCustomSpec() {
		Map<String, String> context = new HashMap<>();
		context.put("a", "01234567890123456789012345678901");
		context.put("b", "0123456789012345");
		String formatted = CorrelationIdFormatter.of("a(32),b(16)").format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void formatToWithDefaultSpec() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		context.put("spanId", "0123456789012345");
		StringBuilder formatted = new StringBuilder();
		CorrelationIdFormatter.DEFAULT.formatTo(context::get, formatted);
		assertThat(formatted).hasToString("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void ofWhenSpecIsMalformed() {
		assertThatIllegalStateException().isThrownBy(() -> CorrelationIdFormatter.of("good(12),bad"))
			.withMessage("Unable to parse correlation formatter spec 'good(12),bad'")
			.havingCause()
			.withMessage("Invalid specification part 'bad'");
	}

	@Test
	void ofWhenSpecIsEmpty() {
		assertThat(CorrelationIdFormatter.of("")).isSameAs(CorrelationIdFormatter.DEFAULT);
	}

	@Test
	void toStringReturnsSpec() {
		assertThat(CorrelationIdFormatter.DEFAULT).hasToString("traceId(32),spanId(16)");
		assertThat(CorrelationIdFormatter.of("a(32),b(16)")).hasToString("a(32),b(16)");
	}

}
