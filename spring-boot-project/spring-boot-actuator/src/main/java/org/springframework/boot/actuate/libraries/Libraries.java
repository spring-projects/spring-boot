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

package org.springframework.boot.actuate.libraries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.util.Assert;

/**
 * Models libraries used by the application.
 *
 * <p>
 * Libraries are grouped by categories, such as {@code "bundled"} or {@code "runtime"}.
 * </p>
 *
 * <p>
 * Each library is represented as a {@link Map Map&lt;String, Object&gt;} containing the
 * details of the library. For example, Java libraries with coordinates in a Maven
 * repository typically contain entries for {@code "groupId"}, {@code "artifactId"}, and
 * {@code "version"}.
 * </p>
 *
 * @author Phil Clay
 * @since 2.5.0
 */
@JsonInclude(Include.NON_EMPTY)
public final class Libraries {

	/**
	 * A map of library category to a list of libraries in that category.
	 *
	 * <p>
	 * For example, the {@code "bundled"} category would contain all of the libraries that
	 * were bundled in the spring boot application at build time.
	 * </p>
	 *
	 * <p>
	 * Each library within a category is represented as a {@link Map} containing the
	 * details of the library. For example, coordinates for libraries in a maven
	 * repository are in the {@code "groupId"}, {@code "artifactId"}, and
	 * {@code "version"} entries in the library details map.
	 * </p>
	 */
	private final Map<String, List<Map<String, Object>>> details;

	private Libraries(Map<String, List<Map<String, Object>>> details) {
		this.details = details;
	}

	/**
	 * Gets the details of the libraries used by the application as a map of library
	 * category to a list of libraries in that category.
	 *
	 * <p>
	 * For example, the {@code "bundled"} category would contain all of the libraries that
	 * were bundled in the spring boot application at build time.
	 * </p>
	 *
	 * <p>
	 * Each library within a category is represented as a {@link Map} containing the
	 * details of the library. For example, coordinates for libraries in a maven
	 * repository are in the {@code "groupId"}, {@code "artifactId"}, and
	 * {@code "version"} entries in the library details map.
	 * </p>
	 * @return the details of the libraries used by the application.
	 */
	@JsonAnyGetter
	public Map<String, List<Map<String, Object>>> getDetails() {
		return this.details;
	}

	/**
	 * Returns the libraries for the given category.
	 *
	 * <p>
	 * Each library within a category is represented as a {@link Map} containing the
	 * details of the library. For example, coordinates for libraries in a maven
	 * repository are in the {@code "groupId"}, {@code "artifactId"}, and
	 * {@code "version"} entries in the library details map.
	 * </p>
	 * @param category the category of libraries to retrieve (For example:
	 * {@code "build"})
	 * @return the libraries for the given category.
	 */
	public List<Map<String, Object>> get(String category) {
		return this.details.get(category);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Libraries libraries = (Libraries) o;
		return this.details.equals(libraries.details);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.details);
	}

	@Override
	public String toString() {
		return getDetails().toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating immutable {@link Libraries} instances.
	 */
	public static class Builder {

		private final Map<String, List<Map<String, Object>>> details = new HashMap<>();

		private final List<Predicate<Map<String, Object>>> includesFilters = new ArrayList<>();

		/**
		 * Adds the given library to the given category.
		 *
		 * <p>
		 * The library map contains the details of the library. For example, coordinates
		 * for libraries in a maven repository would be in the {@code "groupId"},
		 * {@code "artifactId"}, and {@code "version"} entries in the library details map.
		 * </p>
		 * @param category the category of library to add (For example: {@code "bundled"})
		 * @param library the library to add
		 * @return this builder
		 */
		public Builder addLibrary(String category, Map<String, Object> library) {
			Assert.notNull(category, "category must not be null");
			Assert.notNull(library, "library must not be null");
			Assert.notEmpty(library, "library must not be empty");
			this.details.computeIfAbsent(category, (cat) -> new ArrayList<>()).add(library);
			return this;
		}

		/**
		 * Adds all of the given libraries to the given category.
		 * @param category the category of libraries to add (For example:
		 * {@code "bundled"})
		 * @param libraries the libraries to add
		 * @return this builder
		 */
		public Builder addLibraries(String category, List<Map<String, Object>> libraries) {
			Assert.notNull(category, "category must not be null");
			Assert.notNull(libraries, "libraries must not be null");
			libraries.forEach((library) -> addLibrary(category, library));
			return this;
		}

		/**
		 * Adds a filter that determines which libraries that have been added to the
		 * builder will be included in the final {@link Libraries} object.
		 *
		 * <p>
		 * If multiple includes filters are added, then a library must match all filters
		 * to be included in the final {@link Libraries} object
		 * </p>
		 * @param includesFilter a filter that determines which libraries that have been
		 * added to the builder will be included in the final {@link Libraries} object.
		 * @return this builder
		 */
		public Builder addIncludesFilter(Predicate<Map<String, Object>> includesFilter) {
			this.includesFilters.add(includesFilter);
			return this;
		}

		/**
		 * Create a new {@link Libraries} instance based on the state of this builder.
		 * @return a new {@link Libraries} instance
		 */
		public Libraries build() {
			Map<String, List<Map<String, Object>>> details = Collections.unmodifiableMap(this.details.entrySet()
					.stream().sorted(Entry.comparingByKey())
					.collect(Collectors.toMap(Entry::getKey, (entry) -> entry.getValue().stream()
							.filter((libraryMap) -> this.includesFilters.isEmpty()
									|| this.includesFilters.stream().allMatch((filter) -> filter.test(libraryMap)))
							.collect(Collectors.toList()), (u, v) -> {
								throw new IllegalStateException(String.format("Duplicate key %s", u));
							}, LinkedHashMap::new)));
			return new Libraries(details);
		}

	}

}
