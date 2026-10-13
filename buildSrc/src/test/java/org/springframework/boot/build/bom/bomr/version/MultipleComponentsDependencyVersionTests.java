/*
 * Copyright 2012-present the original author or authors.
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
