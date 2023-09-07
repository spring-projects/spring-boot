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
 * Tests for {@link ArtifactVersionDependencyVersion}.
 *
 * @author Andy Wilkinson
 */
class ArtifactVersionDependencyVersionTests {

	@Test
	void parseWhenVersionIsNotAMavenVersionShouldReturnNull() {
		assertThat(version("1.2.3.1")).isNull();
	}

	@Test
	void parseWhenVersionIsAMavenVersionShouldReturnAVersion() {
		assertThat(version("1.2.3")).isNotNull();
	}

	@Test
	void isSameMajorWhenSameMajorAndMinorShouldReturnTrue() {
		assertThat(version("1.10.2").isSameMajor(version("1.10.0"))).isTrue();
	}

	@Test
	void isSameMajorWhenSameMajorShouldReturnTrue() {
		assertThat(version("1.10.2").isSameMajor(version("1.9.0"))).isTrue();
	}

	@Test
	void isSameMajorWhenDifferentMajorShouldReturnFalse() {
		assertThat(version("2.0.2").isSameMajor(version("1.9.0"))).isFalse();
	}

	@Test
	void isSameMinorWhenSameMinorShouldReturnTrue() {
		assertThat(version("1.10.2").isSameMinor(version("1.10.1"))).isTrue();
	}

	@Test
	void isSameMinorWhenDifferentMinorShouldReturnFalse() {
		assertThat(version("1.10.2").isSameMinor(version("1.9.1"))).isFalse();
	}

	@Test
	void isSnapshotForWhenSnapshotForReleaseShouldReturnTrue() {
		assertThat(version("1.10.2-SNAPSHOT").isSnapshotFor(version("1.10.2"))).isTrue();
	}

	@Test
	void isSnapshotForWhenBuildSnapshotForReleaseShouldReturnTrue() {
		assertThat(version("1.10.2.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.RELEASE"))).isTrue();
	}

	@Test
	void isSnapshotForWhenSnapshotForReleaseCandidateShouldReturnTrue() {
		assertThat(version("1.10.2-SNAPSHOT").isSnapshotFor(version("1.10.2-RC2"))).isTrue();
	}

	@Test
	void isSnapshotForWhenBuildSnapshotForReleaseCandidateShouldReturnTrue() {
		assertThat(version("1.10.2.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.RC2"))).isTrue();
	}

	@Test
	void isSnapshotForWhenSnapshotForMilestoneShouldReturnTrue() {
		assertThat(version("1.10.2-SNAPSHOT").isSnapshotFor(version("1.10.2-M1"))).isTrue();
	}

	@Test
	void isSnapshotForWhenBuildSnapshotForMilestoneShouldReturnTrue() {
		assertThat(version("1.10.2.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.M1"))).isTrue();
	}

	@Test
	void isSnapshotForWhenSnapshotForDifferentReleaseShouldReturnFalse() {
		assertThat(version("1.10.1-SNAPSHOT").isSnapshotFor(version("1.10.2"))).isFalse();
	}

	@Test
	void isSnapshotForWhenBuildSnapshotForDifferentReleaseShouldReturnTrue() {
		assertThat(version("1.10.1.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.RELEASE"))).isFalse();
	}

	@Test
	void isSnapshotForWhenSnapshotForDifferentReleaseCandidateShouldReturnTrue() {
		assertThat(version("1.10.1-SNAPSHOT").isSnapshotFor(version("1.10.2-RC2"))).isFalse();
	}

	@Test
	void isSnapshotForWhenBuildSnapshotForDifferentReleaseCandidateShouldReturnTrue() {
		assertThat(version("1.10.1.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.RC2"))).isFalse();
	}

	@Test
	void isSnapshotForWhenSnapshotForDifferentMilestoneShouldReturnTrue() {
		assertThat(version("1.10.1-SNAPSHOT").isSnapshotFor(version("1.10.2-M1"))).isFalse();
	}

	@Test
	void isSnapshotForWhenBuildSnapshotForDifferentMilestoneShouldReturnTrue() {
		assertThat(version("1.10.1.BUILD-SNAPSHOT").isSnapshotFor(version("1.10.2.M1"))).isFalse();
	}

	@Test
	void isSnapshotForWhenNotSnapshotShouldReturnFalse() {
		assertThat(version("1.10.1-M1").isSnapshotFor(version("1.10.1"))).isFalse();
	}

	private ArtifactVersionDependencyVersion version(String version) {
		return ArtifactVersionDependencyVersion.parse(version);
	}

}
