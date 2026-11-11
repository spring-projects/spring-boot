/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigTreeConfigDataResource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigTreeConfigDataResourceTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void constructorWhenPathStringIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreeConfigDataResource((String) null))
			.withMessage("'path' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void constructorWhenPathIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreeConfigDataResource((Path) null))
			.withMessage("'path' must not be null");
	}

	@Test
	void equalsWhenPathIsTheSameReturnsTrue() {
		ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource("/etc/config");
		ConfigTreeConfigDataResource other = new ConfigTreeConfigDataResource("/etc/config");
		assertThat(location).isEqualTo(other);
	}

	@Test
	void equalsWhenPathIsDifferentReturnsFalse() {
		ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource("/etc/config");
		ConfigTreeConfigDataResource other = new ConfigTreeConfigDataResource("other-location");
		assertThat(location).isNotEqualTo(other);
	}

	@Test
	void toStringReturnsDescriptiveString() {
		ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource("/etc/config");
		assertThat(location).hasToString("config tree [" + new File("/etc/config").getAbsolutePath() + "]");
	}

}
