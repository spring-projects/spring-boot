/*
 * Copyright 2012-2020 the original author or authors.
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
public class ArtifactVersionDependencyVersionTests {

	@Test
	void parseWhenVersionIsNotAMavenVersionShouldReturnNull() {
		assertThat(version("1.2.3.1")).isNull();
	}

	@Test
	void parseWhenVersionIsAMavenVersionShouldReturnAVersion() {
		assertThat(version("1.2.3")).isNotNull();
	}

	@Test
	void isNewerThanWhenInputIsOlderMajorShouldReturnTrue() {
		assertThat(version("2.1.2").isNewerThan(version("1.9.0"))).isTrue();
	}

	@Test
	void isNewerThanWhenInputIsOlderMinorShouldReturnTrue() {
		assertThat(version("2.1.2").isNewerThan(version("2.0.2"))).isTrue();
	}

	@Test
	void isNewerThanWhenInputIsOlderPatchShouldReturnTrue() {
		assertThat(version("2.1.2").isNewerThan(version("2.1.1"))).isTrue();
	}

	@Test
	void isNewerThanWhenInputIsNewerMajorShouldReturnFalse() {
		assertThat(version("2.1.2").isNewerThan(version("3.2.1"))).isFalse();
	}

	@Test
	void isSameMajorAndNewerThanWhenMinorIsOlderShouldReturnTrue() {
		assertThat(version("1.10.2").isSameMajorAndNewerThan(version("1.9.0"))).isTrue();
	}

	@Test
	void isSameMajorAndNewerThanWhenMajorIsOlderShouldReturnFalse() {
		assertThat(version("2.0.2").isSameMajorAndNewerThan(version("1.9.0"))).isFalse();
	}

	@Test
	void isSameMajorAndNewerThanWhenPatchIsNewerShouldReturnTrue() {
		assertThat(version("2.1.2").isSameMajorAndNewerThan(version("2.1.1"))).isTrue();
	}

	@Test
	void isSameMajorAndNewerThanWhenMinorIsNewerShouldReturnFalse() {
		assertThat(version("2.1.2").isSameMajorAndNewerThan(version("2.2.1"))).isFalse();
	}

	@Test
	void isSameMajorAndNewerThanWhenMajorIsNewerShouldReturnFalse() {
		assertThat(version("2.1.2").isSameMajorAndNewerThan(version("3.0.1"))).isFalse();
	}

	@Test
	void isSameMinorAndNewerThanWhenPatchIsOlderShouldReturnTrue() {
		assertThat(version("1.10.2").isSameMinorAndNewerThan(version("1.10.1"))).isTrue();
	}

	@Test
	void isSameMinorAndNewerThanWhenMinorIsOlderShouldReturnFalse() {
		assertThat(version("2.1.2").isSameMinorAndNewerThan(version("2.0.1"))).isFalse();
	}

	@Test
	void isSameMinorAndNewerThanWhenVersionsAreTheSameShouldReturnFalse() {
		assertThat(version("2.1.2").isSameMinorAndNewerThan(version("2.1.2"))).isFalse();
	}

	@Test
	void isSameMinorAndNewerThanWhenPatchIsNewerShouldReturnFalse() {
		assertThat(version("2.1.2").isSameMinorAndNewerThan(version("2.1.3"))).isFalse();
	}

	@Test
	void isSameMinorAndNewerThanWhenMinorIsNewerShouldReturnFalse() {
		assertThat(version("2.1.2").isSameMinorAndNewerThan(version("2.0.1"))).isFalse();
	}

	@Test
	void isSameMinorAndNewerThanWhenMajorIsNewerShouldReturnFalse() {
		assertThat(version("3.1.2").isSameMinorAndNewerThan(version("2.0.1"))).isFalse();
	}

	private ArtifactVersionDependencyVersion version(String version) {
		return ArtifactVersionDependencyVersion.parse(version);
	}

}
