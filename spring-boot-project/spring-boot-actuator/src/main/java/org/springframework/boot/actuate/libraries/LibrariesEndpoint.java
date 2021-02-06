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

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to report details about the libraries used by the
 * application.
 *
 * @author Phil Clay
 * @since 2.5.0
 */
@Endpoint(id = "libraries")
public class LibrariesEndpoint {

	private final List<LibrariesContributor> librariesContributors;

	/**
	 * Create a new {@link LibrariesEndpoint} instance.
	 * @param librariesContributors the contributors that populate the Libraries reported
	 * by this endpoint.
	 */
	public LibrariesEndpoint(List<LibrariesContributor> librariesContributors) {
		Assert.notNull(librariesContributors, "Libraries contributors must not be null");
		this.librariesContributors = librariesContributors;
	}

	/**
	 * Return the contributed libraries that match the given include filters.
	 * @param include list of {@code key:value} strings used to determine which
	 * contributed libraries are included. For example, an entry of
	 * {@code groupId:com.example} causes the returned libraries to only include the
	 * libraries with {@code groupId} equal to {@code com.example}. When null or empty,
	 * all contributed libraries will be included.
	 * @return the contributed libraries that match the given include filters.
	 */
	@ReadOperation
	public Libraries libraries(@Nullable List<String> include) {
		Libraries.Builder builder = Libraries.builder();
		this.librariesContributors.forEach((contributor) -> contributor.contribute(builder));
		if (include != null) {
			include.stream().map(this::createPredicateForIncludeFilterString).forEach(builder::addIncludesFilter);
		}
		return builder.build();
	}

	private Predicate<Map<String, Object>> createPredicateForIncludeFilterString(String keyValue) {
		int colonIndex = keyValue.indexOf(":");
		if (colonIndex == -1) {
			// The includes entry is not in key:value format, so exclude everything
			return (Map<String, Object> library) -> false;
		}
		String key = keyValue.substring(0, colonIndex);
		String value = keyValue.substring(colonIndex + 1);
		return (Map<String, Object> library) -> value.equals(library.get(key));
	}

}
