/*
 * Copyright 2012-2022 the original author or authors.
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

	/**
     * Constructs a new IncludeExcludeGroupMemberPredicate with the specified include and exclude sets.
     * 
     * @param include the set of strings to include in the group
     * @param exclude the set of strings to exclude from the group
     */
    IncludeExcludeGroupMemberPredicate(Set<String> include, Set<String> exclude) {
		this.include = clean(include);
		this.exclude = clean(exclude);
	}

	/**
     * Tests if a given name is included in the group and not excluded.
     * 
     * @param name the name to be tested
     * @return true if the name is included and not excluded, false otherwise
     */
    @Override
	public boolean test(String name) {
		name = clean(name);
		return isIncluded(name) && !isExcluded(name);
	}

	/**
     * Checks if the given name is included in the include list.
     * 
     * @param name the name to be checked
     * @return true if the name is included, false otherwise
     */
    private boolean isIncluded(String name) {
		return this.include.isEmpty() || this.include.contains("*") || isIncludedName(name);
	}

	/**
     * Checks if the given name is included in the include list.
     * 
     * @param name the name to check
     * @return true if the name is included, false otherwise
     */
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

	/**
     * Checks if a given name is excluded.
     * 
     * @param name the name to be checked
     * @return true if the name is excluded, false otherwise
     */
    private boolean isExcluded(String name) {
		return this.exclude.contains("*") || isExcludedName(name);
	}

	/**
     * Checks if a given name is excluded.
     * 
     * @param name the name to check
     * @return true if the name is excluded, false otherwise
     */
    private boolean isExcludedName(String name) {
		if (this.exclude.contains(name)) {
			return true;
		}
		if (name.contains("/")) {
			String parent = name.substring(0, name.lastIndexOf("/"));
			return isExcludedName(parent);
		}
		return false;
	}

	/**
     * Cleans the given set of names by removing any null values and duplicates.
     * 
     * @param names the set of names to be cleaned (can be null)
     * @return a cleaned set of names with null values and duplicates removed,
     *         or an empty set if the input set is null
     */
    private Set<String> clean(Set<String> names) {
		if (names == null) {
			return Collections.emptySet();
		}
		Set<String> cleaned = names.stream().map(this::clean).collect(Collectors.toCollection(LinkedHashSet::new));
		return Collections.unmodifiableSet(cleaned);
	}

	/**
     * Cleans the given name by removing leading and trailing whitespace.
     * 
     * @param name the name to be cleaned
     * @return the cleaned name, or null if the input is null
     */
    private String clean(String name) {
		return (name != null) ? name.trim() : null;
	}

}
