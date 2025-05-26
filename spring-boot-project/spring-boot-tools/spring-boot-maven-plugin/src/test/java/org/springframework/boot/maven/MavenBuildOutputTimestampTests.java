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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MavenBuildOutputTimestamp}.
 *
 * @author Moritz Halbritter
 */
class MavenBuildOutputTimestampTests {

	@Test
	void shouldParseNull() {
		assertThat(parse(null)).isNull();
	}

	@Test
	void shouldParseSingleDigit() {
		assertThat(parse("0")).isEqualTo(Instant.parse("1970-01-01T00:00:00Z"));
	}

	@Test
	void shouldNotParseSingleCharacter() {
		assertThat(parse("a")).isNull();
	}

	@Test
	void shouldParseIso8601() {
		assertThat(parse("2011-12-03T10:15:30Z")).isEqualTo(Instant.parse("2011-12-03T10:15:30.000Z"));
	}

	@Test
	void shouldParseIso8601WithMilliseconds() {
		assertThat(parse("2011-12-03T10:15:30.123Z")).isEqualTo(Instant.parse("2011-12-03T10:15:30.123Z"));
	}

	@Test
	void shouldFailIfIso8601BeforeMin() {
		assertThatIllegalArgumentException().isThrownBy(() -> parse("1970-01-01T00:00:00Z"))
			.withMessage(
					"'1970-01-01T00:00:00Z' is not within the valid range 1980-01-01T00:00:02Z to 2099-12-31T23:59:59Z");
	}

	@Test
	void shouldFailIfIso8601AfterMax() {
		assertThatIllegalArgumentException().isThrownBy(() -> parse("2100-01-01T00:00:00Z"))
			.withMessage(
					"'2100-01-01T00:00:00Z' is not within the valid range 1980-01-01T00:00:02Z to 2099-12-31T23:59:59Z");
	}

	@Test
	void shouldFailIfNotIso8601() {
		assertThatIllegalArgumentException().isThrownBy(() -> parse("dummy"))
			.withMessage("Can't parse 'dummy' to instant");
	}

	@Test
	void shouldParseIso8601WithOffset() {
		assertThat(parse("2019-10-05T20:37:42+06:00")).isEqualTo(Instant.parse("2019-10-05T14:37:42Z"));
	}

	@Test
	void shouldParseToFileTime() {
		assertThat(parseFileTime(null)).isEqualTo(null);
		assertThat(parseFileTime("0")).isEqualTo(FileTime.fromMillis(0));
		assertThat(parseFileTime("2019-10-05T14:37:42Z")).isEqualTo(FileTime.fromMillis(1570286262000L));
	}

	private static Instant parse(String timestamp) {
		return new MavenBuildOutputTimestamp(timestamp).toInstant();
	}

	private static FileTime parseFileTime(String timestamp) {
		return new MavenBuildOutputTimestamp(timestamp).toFileTime();
	}

}
