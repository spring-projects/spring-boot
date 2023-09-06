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
 * Tests for {@link MultipleComponentsDependencyVersion}.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
class MultipleComponentsDependencyVersionTests {

	@Test
	void isSameMajorOfFiveComponentVersionWithSameMajorShouldReturnTrue() {
		assertThat(version("21.4.0.0.1").isSameMajor(version("21.1.0.0"))).isTrue();
	}

	@Test
	void isSameMajorOfFiveComponentVersionWithDifferentMajorShouldReturnFalse() {
		assertThat(version("21.4.0.0.1").isSameMajor(version("22.1.0.0"))).isFalse();
	}

	@Test
	void isSameMinorOfFiveComponentVersionWithSameMinorShouldReturnTrue() {
		assertThat(version("21.4.0.0.1").isSameMinor(version("21.4.0.0"))).isTrue();
	}

	@Test
	void isSameMinorOfFiveComponentVersionWithDifferentMinorShouldReturnFalse() {
		assertThat(version("21.4.0.0.1").isSameMinor(version("21.5.0.0"))).isFalse();
	}

	private MultipleComponentsDependencyVersion version(String version) {
		return MultipleComponentsDependencyVersion.parse(version);
	}

}
