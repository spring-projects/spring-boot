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

package org.springframework.boot.buildpack.platform.build;

import java.util.LinkedHashMap;
import java.util.Map;

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
		Map<String, String> env = new LinkedHashMap<>();
		env.put("CNB_USER_ID", "123");
		env.put("CNB_GROUP_ID", "456");
		BuildOwner owner = BuildOwner.fromEnv(env);
		assertThat(owner.getUid()).isEqualTo(123);
		assertThat(owner.getGid()).isEqualTo(456);
		assertThat(owner).hasToString("123/456");
	}

	@Test
	void fromEnvWhenEnvIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildOwner.fromEnv(null))
			.withMessage("Env must not be null");
	}

	@Test
	void fromEnvWhenUserPropertyIsMissingThrowsException() {
		Map<String, String> env = new LinkedHashMap<>();
		env.put("CNB_GROUP_ID", "456");
		assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
			.withMessage("Missing 'CNB_USER_ID' value from the builder environment '" + env + "'");
	}

	@Test
	void fromEnvWhenGroupPropertyIsMissingThrowsException() {
		Map<String, String> env = new LinkedHashMap<>();
		env.put("CNB_USER_ID", "123");
		assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
			.withMessage("Missing 'CNB_GROUP_ID' value from the builder environment '" + env + "'");
	}

	@Test
	void fromEnvWhenUserPropertyIsMalformedThrowsException() {
		Map<String, String> env = new LinkedHashMap<>();
		env.put("CNB_USER_ID", "nope");
		env.put("CNB_GROUP_ID", "456");
		assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
			.withMessage("Malformed 'CNB_USER_ID' value 'nope' in the builder environment '" + env + "'");
	}

	@Test
	void fromEnvWhenGroupPropertyIsMalformedThrowsException() {
		Map<String, String> env = new LinkedHashMap<>();
		env.put("CNB_USER_ID", "123");
		env.put("CNB_GROUP_ID", "nope");
		assertThatIllegalStateException().isThrownBy(() -> BuildOwner.fromEnv(env))
			.withMessage("Malformed 'CNB_GROUP_ID' value 'nope' in the builder environment '" + env + "'");
	}

}
