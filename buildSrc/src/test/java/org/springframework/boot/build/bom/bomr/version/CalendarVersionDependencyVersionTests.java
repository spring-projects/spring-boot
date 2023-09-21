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

package org.springframework.boot.build.bom.bomr.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CalendarVersionDependencyVersion}.
 *
 * @author Andy Wilkinson
 */
class CalendarVersionDependencyVersionTests {

	@Test
	void parseWhenVersionIsNotACalendarVersionShouldReturnNull() {
		assertThat(version("1.2.3")).isNull();
	}

	@Test
	void parseWhenVersionIsACalendarVersionShouldReturnAVersion() {
		assertThat(version("2020.0.0")).isNotNull();
	}

	@Test
	void isSameMajorWhenSameMajorAndMinorShouldReturnTrue() {
		assertThat(version("2020.0.0").isSameMajor(version("2020.0.1"))).isTrue();
	}

	@Test
	void isSameMajorWhenSameMajorShouldReturnTrue() {
		assertThat(version("2020.0.0").isSameMajor(version("2020.1.0"))).isTrue();
	}

	@Test
	void isSameMajorWhenDifferentMajorShouldReturnFalse() {
		assertThat(version("2020.0.0").isSameMajor(version("2021.0.0"))).isFalse();
	}

	@Test
	void isSameMinorWhenSameMinorShouldReturnTrue() {
		assertThat(version("2020.0.0").isSameMinor(version("2020.0.1"))).isTrue();
	}

	@Test
	void isSameMinorWhenDifferentMinorShouldReturnFalse() {
		assertThat(version("2020.0.0").isSameMinor(version("2020.1.0"))).isFalse();
	}

	@Test
	void calendarVersionIsNotSameMajorAsReleaseTrainVersion() {
		assertThat(version("2020.0.0").isSameMajor(releaseTrainVersion("Aluminium-RELEASE"))).isFalse();
	}

	@Test
	void calendarVersionIsNotSameMinorAsReleaseTrainVersion() {
		assertThat(version("2020.0.0").isSameMinor(releaseTrainVersion("Aluminium-RELEASE"))).isFalse();
	}

	private ReleaseTrainDependencyVersion releaseTrainVersion(String version) {
		return ReleaseTrainDependencyVersion.parse(version);
	}

	private CalendarVersionDependencyVersion version(String version) {
		return CalendarVersionDependencyVersion.parse(version);
	}

}
