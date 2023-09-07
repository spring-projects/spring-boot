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
 * Tests for {@link ReleaseTrainDependencyVersion}.
 *
 * @author Andy Wilkinson
 */
class ReleaseTrainDependencyVersionTests {

	@Test
	void parsingOfANonReleaseTrainVersionReturnsNull() {
		assertThat(version("5.1.4.RELEASE")).isNull();
	}

	@Test
	void parsingOfAReleaseTrainVersionReturnsVersion() {
		assertThat(version("Lovelace-SR3")).isNotNull();
	}

	@Test
	void isSameMajorWhenReleaseTrainIsDifferentShouldReturnFalse() {
		assertThat(version("Lovelace-RELEASE").isSameMajor(version("Kay-SR5"))).isFalse();
	}

	@Test
	void isSameMajorWhenReleaseTrainIsTheSameShouldReturnTrue() {
		assertThat(version("Lovelace-RELEASE").isSameMajor(version("Lovelace-SR5"))).isTrue();
	}

	@Test
	void isSameMinorWhenReleaseTrainIsDifferentShouldReturnFalse() {
		assertThat(version("Lovelace-RELEASE").isSameMajor(version("Kay-SR5"))).isFalse();
	}

	@Test
	void isSameMinorWhenReleaseTrainIsTheSameShouldReturnTrue() {
		assertThat(version("Lovelace-RELEASE").isSameMajor(version("Lovelace-SR5"))).isTrue();
	}

	@Test
	void releaseTrainVersionIsNotSameMajorAsCalendarTrainVersion() {
		assertThat(version("Kay-SR6").isSameMajor(calendarVersion("2020.0.0"))).isFalse();
	}

	@Test
	void releaseTrainVersionIsNotSameMinorAsCalendarVersion() {
		assertThat(version("Kay-SR6").isSameMinor(calendarVersion("2020.0.0"))).isFalse();
	}

	@Test
	void isSnapshotForWhenSnapshotForServiceReleaseShouldReturnTrue() {
		assertThat(version("Kay-BUILD-SNAPSHOT").isSnapshotFor(version("Kay-SR2"))).isTrue();
	}

	@Test
	void isSnapshotForWhenSnapshotForReleaseShouldReturnTrue() {
		assertThat(version("Kay-BUILD-SNAPSHOT").isSnapshotFor(version("Kay-RELEASE"))).isTrue();
	}

	@Test
	void isSnapshotForWhenSnapshotForReleaseCandidateShouldReturnTrue() {
		assertThat(version("Kay-BUILD-SNAPSHOT").isSnapshotFor(version("Kay-RC1"))).isTrue();
	}

	@Test
	void isSnapshotForWhenSnapshotForMilestoneShouldReturnTrue() {
		assertThat(version("Kay-BUILD-SNAPSHOT").isSnapshotFor(version("Kay-M2"))).isTrue();
	}

	@Test
	void isSnapshotForWhenSnapshotForDifferentReleaseShouldReturnFalse() {
		assertThat(version("Kay-BUILD-SNAPSHOT").isSnapshotFor(version("Lovelace-RELEASE"))).isFalse();
	}

	@Test
	void isSnapshotForWhenSnapshotForDifferentReleaseCandidateShouldReturnTrue() {
		assertThat(version("Kay-BUILD-SNAPSHOT").isSnapshotFor(version("Lovelace-RC2"))).isFalse();
	}

	@Test
	void isSnapshotForWhenSnapshotForDifferentMilestoneShouldReturnTrue() {
		assertThat(version("Kay-BUILD-SNAPSHOT").isSnapshotFor(version("Lovelace-M1"))).isFalse();
	}

	@Test
	void isSnapshotForWhenNotSnapshotShouldReturnFalse() {
		assertThat(version("Kay-M1").isSnapshotFor(version("Kay-RELEASE"))).isFalse();
	}

	private static ReleaseTrainDependencyVersion version(String input) {
		return ReleaseTrainDependencyVersion.parse(input);
	}

	private CalendarVersionDependencyVersion calendarVersion(String version) {
		return CalendarVersionDependencyVersion.parse(version);
	}

}
