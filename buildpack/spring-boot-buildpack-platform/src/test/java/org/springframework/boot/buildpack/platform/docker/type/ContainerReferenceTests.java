/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ContainerReference}.
 *
 * @author Phillip Webb
 */
class ContainerReferenceTests {

	@Test
	void ofCreatesInstance() {
		ContainerReference reference = ContainerReference
			.of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
		assertThat(reference).hasToString("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerReference.of(null))
			.withMessage("'value' must not be empty");
	}

	@Test
	void ofWhenEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerReference.of(""))
			.withMessage("'value' must not be empty");
	}

	@Test
	void hashCodeAndEquals() {
		ContainerReference r1 = ContainerReference
			.of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
		ContainerReference r2 = ContainerReference
			.of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
		ContainerReference r3 = ContainerReference
			.of("02691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
		assertThat(r1).hasSameHashCodeAs(r2);
		assertThat(r1).isEqualTo(r1).isEqualTo(r2).isNotEqualTo(r3);
	}

}
