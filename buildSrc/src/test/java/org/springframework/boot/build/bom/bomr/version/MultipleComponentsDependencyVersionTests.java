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
	void isNewerThanOnVersionWithNumericQualifierWhenInputHasNoQualifierShouldReturnTrue() {
		assertThat(version("2.9.9.20190806").isNewerThan(DependencyVersion.parse("2.9.9"))).isTrue();
	}

	@Test
	void isNewerThanOnVersionWithNumericQualifierWhenInputHasOlderQualifierShouldReturnTrue() {
		assertThat(version("2.9.9.20190806").isNewerThan(version("2.9.9.20190805"))).isTrue();
	}

	@Test
	void isNewerThanOnVersionWithNumericQualifierWhenInputHasNewerQualifierShouldReturnFalse() {
		assertThat(version("2.9.9.20190806").isNewerThan(version("2.9.9.20190807"))).isFalse();
	}

	@Test
	void isNewerThanOnVersionWithNumericQualifierWhenInputHasSameQualifierShouldReturnFalse() {
		assertThat(version("2.9.9.20190806").isNewerThan(version("2.9.9.20190806"))).isFalse();
	}

	@Test
	void isNewerThanWorksWith5Components() {
		assertThat(version("21.4.0.0.1").isNewerThan(version("21.1.0.0"))).isTrue();
	}

	@Test
	void isNewerThanWorksWith5ComponentsAndLastComponentIsConsidered() {
		assertThat(version("21.1.0.0.1").isNewerThan(version("21.1.0.0"))).isTrue();
	}

	@Test
	void isSameMajorAndNewerThanWorksWith5Components() {
		assertThat(version("21.4.0.0.1").isSameMajorAndNewerThan(version("21.1.0.0"))).isTrue();
	}

	@Test
	void isSameMinorAndNewerThanWorksWith5Components() {
		assertThat(version("21.4.0.0.1").isSameMinorAndNewerThan(version("21.1.0.0"))).isFalse();
	}

	private MultipleComponentsDependencyVersion version(String version) {
		return MultipleComponentsDependencyVersion.parse(version);
	}

}
