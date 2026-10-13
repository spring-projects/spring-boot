/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link LifecycleVersion}.
 *
 * @author Phillip Webb
 */
class LifecycleVersionTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void parseWhenValueIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse(null))
			.withMessage("'value' must not be empty");
	}

	@Test
	void parseWhenTooLongThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse("v1.2.3.4"))
			.withMessage("'value' [v1.2.3.4] must be a valid version number");
	}

	@Test
	void parseWhenNonNumericThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse("v1.2.3a"))
			.withMessage("'value' [v1.2.3a] must be a valid version number");
	}

	@Test
	void compareTo() {
		LifecycleVersion v4 = LifecycleVersion.parse("0.0.4");
		assertThat(LifecycleVersion.parse("0.0.3")).isLessThan(v4);
		assertThat(LifecycleVersion.parse("0.0.4")).isEqualByComparingTo(v4);
		assertThat(LifecycleVersion.parse("0.0.5")).isGreaterThan(v4);
	}

	@Test
	void isEqualOrGreaterThan() {
		LifecycleVersion v4 = LifecycleVersion.parse("0.0.4");
		assertThat(LifecycleVersion.parse("0.0.3").isEqualOrGreaterThan(v4)).isFalse();
		assertThat(LifecycleVersion.parse("0.0.4").isEqualOrGreaterThan(v4)).isTrue();
		assertThat(LifecycleVersion.parse("0.0.5").isEqualOrGreaterThan(v4)).isTrue();
	}

	@Test
	void parseReturnsVersion() {
		assertThat(LifecycleVersion.parse("1.2.3")).hasToString("v1.2.3");
		assertThat(LifecycleVersion.parse("1.2")).hasToString("v1.2.0");
		assertThat(LifecycleVersion.parse("1")).hasToString("v1.0.0");
	}

}
