/*
 * Copyright 2012-present the original author or authors.
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
