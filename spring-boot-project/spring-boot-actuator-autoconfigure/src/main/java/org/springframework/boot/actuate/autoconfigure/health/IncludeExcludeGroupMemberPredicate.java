/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Member predicate that matches based on {@code include} and {@code exclude} sets.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class IncludeExcludeGroupMemberPredicate implements Predicate<String> {

	private final Set<String> include;

	private final Set<String> exclude;

	IncludeExcludeGroupMemberPredicate(Set<String> include, Set<String> exclude) {
		this.include = clean(include);
		this.exclude = clean(exclude);
	}

	@Override
	public boolean test(String name) {
		return testCleanName(clean(name));
	}

	private boolean testCleanName(String name) {
		return isIncluded(name) && !isExcluded(name);
	}

	private boolean isIncluded(String name) {
		return this.include.isEmpty() || this.include.contains("*") || isIncludedName(name);
	}

	private boolean isIncludedName(String name) {
		if (this.include.contains(name)) {
			return true;
		}
		if (name.contains("/")) {
			String parent = name.substring(0, name.lastIndexOf("/"));
			return isIncludedName(parent);
		}
		return false;
	}

	private boolean isExcluded(String name) {
		return this.exclude.contains("*") || this.exclude.contains(name);
	}

	private Set<String> clean(Set<String> names) {
		if (names == null) {
			return Collections.emptySet();
		}
		Set<String> cleaned = names.stream().map(this::clean).collect(Collectors.toCollection(LinkedHashSet::new));
		return Collections.unmodifiableSet(cleaned);
	}

	private String clean(String name) {
		return (name != null) ? name.trim() : null;
	}

}
