/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.observation.autoconfigure;

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
