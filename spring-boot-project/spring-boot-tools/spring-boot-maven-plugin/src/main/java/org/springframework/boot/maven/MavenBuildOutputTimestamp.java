/*
 * Copyright 2012-2024 the original author or authors.
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

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.springframework.util.StringUtils;

/**
 * Parse output timestamp configured for Reproducible Builds' archive entries.
 * <p>
 * Either as {@link java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME} or as a
 * number representing seconds since the epoch (like <a href=
 * "https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
 * Implementation inspired by <a href=
 * "https://github.com/apache/maven-archiver/blob/cc2f6a219f6563f450b0c00e8ccd651520b67406/src/main/java/org/apache/maven/archiver/MavenArchiver.java#L768">MavenArchiver</a>.
 *
 * @author Niels Basjes
 * @author Moritz Halbritter
 */
class MavenBuildOutputTimestamp {

	private static final Instant DATE_MIN = Instant.parse("1980-01-01T00:00:02Z");

	private static final Instant DATE_MAX = Instant.parse("2099-12-31T23:59:59Z");

	private final String timestamp;

	/**
	 * Creates a new {@link MavenBuildOutputTimestamp}.
	 * @param timestamp timestamp or {@code null}
	 */
	MavenBuildOutputTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Returns the parsed timestamp as an {@code FileTime}.
	 * @return the parsed timestamp as an {@code FileTime}, or {@code null}
	 * @throws IllegalArgumentException if the outputTimestamp is neither ISO 8601 nor an
	 * integer, or it's not within the valid range 1980-01-01T00:00:02Z to
	 * 2099-12-31T23:59:59Z
	 */
	FileTime toFileTime() {
		Instant instant = toInstant();
		if (instant == null) {
			return null;
		}
		return FileTime.from(instant);
	}

	/**
	 * Returns the parsed timestamp as an {@code Instant}.
	 * @return the parsed timestamp as an {@code Instant}, or {@code null}
	 * @throws IllegalArgumentException if the outputTimestamp is neither ISO 8601 nor an
	 * integer, or it's not within the valid range 1980-01-01T00:00:02Z to
	 * 2099-12-31T23:59:59Z
	 */
	Instant toInstant() {
		if (!StringUtils.hasLength(this.timestamp)) {
			return null;
		}
		if (isNumeric(this.timestamp)) {
			return Instant.ofEpochSecond(Long.parseLong(this.timestamp));
		}
		if (this.timestamp.length() < 2) {
			return null;
		}
		try {
			Instant instant = OffsetDateTime.parse(this.timestamp).withOffsetSameInstant(ZoneOffset.UTC).toInstant();
			if (instant.isBefore(DATE_MIN) || instant.isAfter(DATE_MAX)) {
				throw new IllegalArgumentException(
						String.format("'%s' is not within the valid range %s to %s", instant, DATE_MIN, DATE_MAX));
			}
			return instant;
		}
		catch (DateTimeParseException pe) {
			throw new IllegalArgumentException(String.format("Can't parse '%s' to instant", this.timestamp));
		}
	}

	private static boolean isNumeric(String str) {
		for (char c : str.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

}
