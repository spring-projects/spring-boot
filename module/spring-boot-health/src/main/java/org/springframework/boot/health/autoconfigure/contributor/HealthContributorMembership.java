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

import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Determines if a health contributor is a member of a group or collection.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.1.0
 */
@FunctionalInterface
public interface HealthContributorMembership {

	/**
	 * Returns {@code true} if the given contributor is a member.
	 * @param name the contributor name
	 * @return {@code true} if the contributor is a member
	 */
	boolean isMember(String name);

	/**
	 * Return a {@link HealthContributorMembership} that always matches.
	 * @return a {@link HealthContributorMembership} instance that always matches
	 */
	static HealthContributorMembership always() {
		return (name) -> true;
	}

	/**
	 * Return a {@link HealthContributorMembership} instance that matches based on include
	 * and exclude patterns. Support {@code '*'} patterns and {@code '/'} separated names.
	 * @param include the include patterns or {@code null}
	 * @param exclude the exclude patterns of {@code null}
	 * @return a new {@link HealthContributorMembership} instance
	 */
	static HealthContributorMembership byIncludeExclude(@Nullable Set<String> include, @Nullable Set<String> exclude) {
		return new IncludeExcludeHealthContributorMembership(include, exclude);
	}

}
