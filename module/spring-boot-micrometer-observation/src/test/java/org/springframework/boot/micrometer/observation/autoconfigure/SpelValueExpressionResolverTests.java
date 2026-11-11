/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.observation.autoconfigure;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SpelValueExpressionResolver}.
 *
 * @author Dominique Villard
 */
class SpelValueExpressionResolverTests {

	final SpelValueExpressionResolver resolver = new SpelValueExpressionResolver();

	@Test
	void checkValidExpression() {
		var value = Map.of("foo", Pair.of(1, 2));
		assertThat(this.resolver.resolve("['foo'].first", value)).isEqualTo("1");
	}

	@Test
	void checkInvalidExpression() {
		var value = Map.of("foo", Pair.of(1, 2));
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve("['bar'].first", value));
	}

	record Pair(int first, int second) {

		static Pair of(int first, int second) {
			return new Pair(first, second);
		}

	}

}
