/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.origin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SystemEnvironmentOrigin}.
 *
 * @author Madhura Bhave
 */
class SystemEnvironmentOriginTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenPropertyIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SystemEnvironmentOrigin(null))
			.withMessage("'property' must not be empty");
	}

	@Test
	void createWhenPropertyNameIsEmptyShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SystemEnvironmentOrigin(""))
			.withMessage("'property' must not be empty");
	}

	@Test
	void getPropertyShouldReturnProperty() {
		SystemEnvironmentOrigin origin = new SystemEnvironmentOrigin("FOO_BAR");
		assertThat(origin.getProperty()).isEqualTo("FOO_BAR");
	}

	@Test
	void toStringShouldReturnStringWithDetails() {
		SystemEnvironmentOrigin origin = new SystemEnvironmentOrigin("FOO_BAR");
		assertThat(origin).hasToString("System Environment Property \"FOO_BAR\"");
	}

}
