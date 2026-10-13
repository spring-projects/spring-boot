/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link BuildOwner}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class BuildOwnerTests {

	@Test
	void fromEnvReturnsOwner() {
		Map<String, @Nullable String> env = new LinkedHashMap<>();
		env.put("CNB_USER_ID", "123");
		env.put("CNB_GROUP_ID", "456");
		BuildOwner owner = BuildOwner.fromEnv(env);
		assertThat(owner.getUid()).isEqualTo(123);
		assertThat(owner.getGid()).isEqualTo(456);
		assertThat(owner).hasToString("123/456");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void fromEnvWhenEnvIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildOwner.fromEnv(null))
			.withMessage("'env' must not be null");
	}

	@Test
	void fromEnvWhenUserPropertyIsMissingThrowsException() {
		Map<String, @Nullable String> env = new LinkedHashMap<>();
		env.put("CNB_GROUP_ID", "456");
		assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
			.withMessage("Missing 'CNB_USER_ID' value from the builder environment '" + env + "'");
	}

	@Test
	void fromEnvWhenGroupPropertyIsMissingThrowsException() {
		Map<String, @Nullable String> env = new LinkedHashMap<>();
		env.put("CNB_USER_ID", "123");
		assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
			.withMessage("Missing 'CNB_GROUP_ID' value from the builder environment '" + env + "'");
	}

	@Test
	void fromEnvWhenUserPropertyIsMalformedThrowsException() {
		Map<String, @Nullable String> env = new LinkedHashMap<>();
		env.put("CNB_USER_ID", "nope");
		env.put("CNB_GROUP_ID", "456");
		assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
			.withMessage("Malformed 'CNB_USER_ID' value 'nope' in the builder environment '" + env + "'");
	}

	@Test
	void fromEnvWhenGroupPropertyIsMalformedThrowsException() {
		Map<String, @Nullable String> env = new LinkedHashMap<>();
		env.put("CNB_USER_ID", "123");
		env.put("CNB_GROUP_ID", "nope");
		assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
			.withMessage("Malformed 'CNB_GROUP_ID' value 'nope' in the builder environment '" + env + "'");
	}

}
