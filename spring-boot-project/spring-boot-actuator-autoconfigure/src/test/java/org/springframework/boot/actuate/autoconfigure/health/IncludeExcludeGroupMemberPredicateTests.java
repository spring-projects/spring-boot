/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IncludeExcludeGroupMemberPredicate}.
 *
 * @author Phillip Webb
 */
class IncludeExcludeGroupMemberPredicateTests {

	@Test
	void testWhenEmptyIncludeAndExcludeRejectsAll() {
		Predicate<String> predicate = new IncludeExcludeGroupMemberPredicate(null, null);
		assertThat(predicate).rejects("a", "b", "c");
	}

	@Test
	void testWhenStarIncludeAndEmptyExcludeAcceptsAll() {
		Predicate<String> predicate = include("*").exclude();
		assertThat(predicate).accepts("a", "b", "c");
	}

	@Test
	void testWhenStarIncludeAndSpecificExcludeDoesNotAcceptExclude() {
		Predicate<String> predicate = include("*").exclude("c");
		assertThat(predicate).accepts("a", "b").rejects("c");
	}

	@Test
	void testWhenSpecificIncludeAcceptsOnlyIncluded() {
		Predicate<String> predicate = include("a", "b").exclude();
		assertThat(predicate).accepts("a", "b").rejects("c");
	}

	@Test
	void testWhenSpecifiedIncludeAndSpecifiedExcludeAcceptsAsExpected() {
		Predicate<String> predicate = include("a", "b", "c").exclude("c");
		assertThat(predicate).accepts("a", "b").rejects("c", "d");
	}

	@Test
	void testWhenSpecifiedIncludeAndStarExcludeRejectsAll() {
		Predicate<String> predicate = include("a", "b", "c").exclude("*");
		assertThat(predicate).rejects("a", "b", "c", "d");
	}

	private Builder include(String... include) {
		return new Builder(include);
	}

	private static class Builder {

		private final String[] include;

		Builder(String[] include) {
			this.include = include;
		}

		Predicate<String> exclude(String... exclude) {
			return new IncludeExcludeGroupMemberPredicate(asSet(this.include), asSet(exclude));
		}

		private Set<String> asSet(String[] names) {
			return (names != null) ? new LinkedHashSet<>(Arrays.asList(names)) : null;
		}

	}

}
