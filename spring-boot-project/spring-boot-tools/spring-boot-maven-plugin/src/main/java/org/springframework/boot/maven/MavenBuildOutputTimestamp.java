/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.maven;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class MavenBuildOutputTimestamp {

	private MavenBuildOutputTimestamp() {
		// Utility class
	}

	// This implementation was copied from the maven-archiver
	// https://github.com/apache/maven-archiver/blob/cc2f6a219f6563f450b0c00e8ccd651520b67406/src/main/java/org/apache/maven/archiver/MavenArchiver.java#L768

	private static final Instant DATE_MIN = Instant.parse("1980-01-01T00:00:02Z");

	private static final Instant DATE_MAX = Instant.parse("2099-12-31T23:59:59Z");

	/**
	 * Parse output timestamp configured for Reproducible Builds' archive entries.
	 *
	 * <p>
	 * Either as {@link java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME} or as a
	 * number representing seconds since the epoch (like <a href=
	 * "https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
	 * @param outputTimestamp the value of {@code ${project.build.outputTimestamp}}
	 * (maybe {@code null})
	 * @return the parsed timestamp as an {@code Optional<Instant>}, {@code empty} if
	 * input is {@code null} or input contains only 1 character (not a number)
	 * @throws IllegalArgumentException if the outputTimestamp is neither ISO 8601 nor an
	 * integer, or it's not within the valid range 1980-01-01T00:00:02Z to
	 * 2099-12-31T23:59:59Z
	 */
	public static Optional<Instant> parseBuildOutputTimestamp(String outputTimestamp) {
		// Fail-fast on nulls
		if (outputTimestamp == null) {
			return Optional.empty();
		}

		// Number representing seconds since the epoch
		if (isNumeric(outputTimestamp)) {
			return Optional.of(Instant.ofEpochSecond(Long.parseLong(outputTimestamp)));
		}

		// no timestamp configured (1 character configuration is useful to override a full
		// value during pom
		// inheritance)
		if (outputTimestamp.length() < 2) {
			return Optional.empty();
		}

		try {
			// Parse the date in UTC such as '2011-12-03T10:15:30Z' or with an offset
			// '2019-10-05T20:37:42+06:00'.
			final Instant date = OffsetDateTime.parse(outputTimestamp)
				.withOffsetSameInstant(ZoneOffset.UTC)
				.truncatedTo(ChronoUnit.SECONDS)
				.toInstant();

			if (date.isBefore(DATE_MIN) || date.isAfter(DATE_MAX)) {
				throw new IllegalArgumentException(
						"'" + date + "' is not within the valid range " + DATE_MIN + " to " + DATE_MAX);
			}
			return Optional.of(date);
		}
		catch (DateTimeParseException pe) {
			throw new IllegalArgumentException("Invalid project.build.outputTimestamp value '" + outputTimestamp + "'",
					pe);
		}
	}

	private static boolean isNumeric(String str) {

		if (str.isEmpty()) {
			return false;
		}

		for (char c : str.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}

		return true;
	}

}
