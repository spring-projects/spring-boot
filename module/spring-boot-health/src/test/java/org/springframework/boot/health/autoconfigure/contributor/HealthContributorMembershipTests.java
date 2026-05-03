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

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthContributorMembership}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class HealthContributorMembershipTests {

	@Nested
	class ByIncludeExcludeTests {

		@Test
		void isMemberWhenEmptyIncludeAndExcludeAcceptsAll() {
			HealthContributorMembership membership = HealthContributorMembership.byIncludeExclude(null, null);
			assertThat(asPredicate(membership)).accepts("a", "b", "c");
		}

		@Test
		void isMemberWhenStarIncludeAndEmptyExcludeAcceptsAll() {
			HealthContributorMembership membership = include("*").exclude();
			assertThat(asPredicate(membership)).accepts("a", "b", "c");
		}

		@Test
		void isMemberWhenEmptyIncludeAndNonEmptyExcludeAcceptsAllButExclude() {
			HealthContributorMembership membership = HealthContributorMembership.byIncludeExclude(null,
					Collections.singleton("c"));
			assertThat(asPredicate(membership)).accepts("a", "b");
		}

		@Test
		void isMemberWhenStarIncludeAndSpecificExcludeDoesNotAcceptExclude() {
			HealthContributorMembership membership = include("*").exclude("c");
			assertThat(asPredicate(membership)).accepts("a", "b").rejects("c");
		}

		@Test
		void isMemberWhenSpecificIncludeAcceptsOnlyIncluded() {
			HealthContributorMembership membership = include("a", "b").exclude();
			assertThat(asPredicate(membership)).accepts("a", "b").rejects("c");
		}

		@Test
		void isMemberWhenSpecifiedIncludeAndSpecifiedExcludeAcceptsAsExpected() {
			HealthContributorMembership membership = include("a", "b", "c").exclude("c");
			assertThat(asPredicate(membership)).accepts("a", "b").rejects("c", "d");
		}

		@Test
		void isMemberWhenSpecifiedIncludeAndStarExcludeRejectsAll() {
			HealthContributorMembership membership = include("a", "b", "c").exclude("*");
			assertThat(asPredicate(membership)).rejects("a", "b", "c", "d");
		}

		@Test
		void isMemberWhenCamelCaseIncludeAcceptsOnlyIncluded() {
			HealthContributorMembership membership = include("myEndpoint").exclude();
			assertThat(asPredicate(membership)).accepts("myEndpoint").rejects("d");
		}

		@Test
		void isMemberWhenHyphenCaseIncludeAcceptsOnlyIncluded() {
			HealthContributorMembership membership = include("my-endpoint").exclude();
			assertThat(asPredicate(membership)).accepts("my-endpoint").rejects("d");
		}

		@Test
		void isMemberWhenExtraWhitespaceAcceptsTrimmedVersion() {
			HealthContributorMembership membership = include("  myEndpoint  ").exclude();
			assertThat(asPredicate(membership)).accepts("myEndpoint").rejects("d");
		}

		@Test
		void isMemberWhenSpecifiedIncludeWithSlash() {
			HealthContributorMembership membership = include("test/a").exclude();
			assertThat(asPredicate(membership)).accepts("test/a").rejects("test").rejects("test/b");
		}

		@Test
		void specifiedIncludeShouldIncludeNested() {
			HealthContributorMembership membership = include("test").exclude();
			assertThat(asPredicate(membership)).accepts("test/a/d").accepts("test/b").rejects("foo");
		}

		@Test
		void specifiedIncludeShouldNotIncludeExcludedNested() {
			HealthContributorMembership membership = include("test").exclude("test/b");
			assertThat(asPredicate(membership)).accepts("test/a").rejects("test/b").rejects("foo");
		}

		@Test // gh-29251
		void specifiedExcludeShouldExcludeNestedChildren() {
			HealthContributorMembership membership = include("*").exclude("test");
			assertThat(asPredicate(membership)).rejects("test").rejects("test/a").rejects("test/a").accepts("other");
		}

		private Predicate<String> asPredicate(HealthContributorMembership membership) {
			return membership::isMember;
		}

		private Builder include(String... include) {
			return new Builder(include);
		}

		private static class Builder {

			private final String[] include;

			Builder(String[] include) {
				this.include = include;
			}

			HealthContributorMembership exclude(String... exclude) {
				return HealthContributorMembership.byIncludeExclude(asSet(this.include), asSet(exclude));
			}

			private @Nullable Set<String> asSet(String @Nullable [] names) {
				return (names != null) ? new LinkedHashSet<>(Arrays.asList(names)) : null;
			}

		}

	}

}
