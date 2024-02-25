/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.boot.build.bom.bomr;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Release schedule for Spring projects, retrieved from
 * <a href="https://calendar.spring.io">https://calendar.spring.io</a>.
 *
 * @author Andy Wilkinson
 */
class ReleaseSchedule {

	private static final Pattern LIBRARY_AND_VERSION = Pattern.compile("([A-Za-z0-9 ]+) ([0-9A-Za-z.-]+)");

	private final RestOperations rest;

	/**
     * Constructs a new ReleaseSchedule object with a default RestTemplate.
     */
    ReleaseSchedule() {
		this(new RestTemplate());
	}

	/**
     * Initializes a new instance of the ReleaseSchedule class.
     * 
     * @param rest the RestOperations object used for making RESTful API calls
     */
    ReleaseSchedule(RestOperations rest) {
		this.rest = rest;
	}

	/**
     * Retrieves a map of releases between the specified start and end dates.
     * 
     * @param start the start date and time
     * @param end the end date and time
     * @return a map of releases grouped by library name
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	Map<String, List<Release>> releasesBetween(OffsetDateTime start, OffsetDateTime end) {
		ResponseEntity<List> response = this.rest
			.getForEntity("https://calendar.spring.io/releases?start=" + start + "&end=" + end, List.class);
		List<Map<String, String>> body = response.getBody();
		Map<String, List<Release>> releasesByLibrary = new LinkedCaseInsensitiveMap<>();
		body.stream()
			.map(this::asRelease)
			.filter(Objects::nonNull)
			.forEach((release) -> releasesByLibrary.computeIfAbsent(release.getLibraryName(), (l) -> new ArrayList<>())
				.add(release));
		return releasesByLibrary;
	}

	/**
     * Converts a map entry into a Release object.
     * 
     * @param entry the map entry containing the release information
     * @return the Release object created from the map entry, or null if the title does not match the expected format
     */
    private Release asRelease(Map<String, String> entry) {
		LocalDate due = LocalDate.parse(entry.get("start"));
		String title = entry.get("title");
		Matcher matcher = LIBRARY_AND_VERSION.matcher(title);
		if (!matcher.matches()) {
			return null;
		}
		String library = matcher.group(1);
		String version = matcher.group(2);
		return new Release(library, DependencyVersion.parse(version), due);
	}

	/**
     * Release class.
     */
    static class Release {

		private final String libraryName;

		private final DependencyVersion version;

		private final LocalDate dueOn;

		/**
         * Creates a new release for a library with the specified name, version, and due date.
         * 
         * @param libraryName the name of the library
         * @param version the version of the library
         * @param dueOn the due date for the release
         */
        Release(String libraryName, DependencyVersion version, LocalDate dueOn) {
			this.libraryName = libraryName;
			this.version = version;
			this.dueOn = dueOn;
		}

		/**
         * Returns the name of the library.
         *
         * @return the name of the library
         */
        String getLibraryName() {
			return this.libraryName;
		}

		/**
         * Returns the version of the dependency.
         *
         * @return the version of the dependency
         */
        DependencyVersion getVersion() {
			return this.version;
		}

		/**
         * Returns the due date of the release.
         *
         * @return the due date of the release
         */
        LocalDate getDueOn() {
			return this.dueOn;
		}

	}

}
