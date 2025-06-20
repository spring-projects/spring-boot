/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.actuate.autoconfigure.tracing.SpelTagValueExpressionResolver;
import org.springframework.data.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SpelTagValueExpressionResolverTest {

	final SpelTagValueExpressionResolver resolver = new SpelTagValueExpressionResolver();

	@ParameterizedTest
	@MethodSource
	void checkValidExpression(Object value, String expression, String expected) {
		assertThat(this.resolver.resolve(expression, value)).isEqualTo(expected);
	}

	static Stream<Arguments> checkValidExpression() {
		return Stream.of(
				Arguments.of("foo", "length", "3"),
				Arguments.of("foo", "isEmpty", "false"),
				Arguments.of(Pair.of("left", "right"), "first", "left"),
				Arguments.of(Map.of("foo", "bar"), "['foo']", "bar"),
				Arguments.of(Map.of("foo", "bar"), "['baz']", null),
				Arguments.of(Map.of("foo", Pair.of(1, 2)), "['foo'].first", "1"),
				Arguments.of(Map.of("foo", Pair.of(1, 2)), "['bar']?.first", null));
	}

	@ParameterizedTest
	@MethodSource
	void checkInvalidExpression(Object value, String expression) {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> this.resolver.resolve(expression, value));
	}

	static Stream<Arguments> checkInvalidExpression() {
		return Stream.of(
				Arguments.of("foo", "unknownMethod"),
				Arguments.of(null, "length"),
				Arguments.of(Map.of("foo", Pair.of(1, 2)), "['bar'].first"),
				Arguments.of(Map.of(), "invalid expression"));
	}

	@Test
	void checkParserReuse() {
		var map = (Map<?, ?>) ReflectionTestUtils.getField(this.resolver,"expressionMap");

		this.resolver.resolve("length", "foo");
		this.resolver.resolve("length", "bar");

		assertThat(map).hasSize(1);

		this.resolver.resolve("isEmpty", "foo");
		assertThat(map).hasSize(2);
	}
}