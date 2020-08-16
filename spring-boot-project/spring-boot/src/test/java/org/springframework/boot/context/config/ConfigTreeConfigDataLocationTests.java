/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigTreeConfigDataLocation}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class ConfigTreeConfigDataLocationTests {

	@Test
	void constructorWhenPathIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreeConfigDataLocation(null))
				.withMessage("Path must not be null");
	}

	@Test
	void equalsWhenPathIsTheSameReturnsTrue() {
		ConfigTreeConfigDataLocation location = new ConfigTreeConfigDataLocation("/etc/config");
		ConfigTreeConfigDataLocation other = new ConfigTreeConfigDataLocation("/etc/config");
		assertThat(location).isEqualTo(other);
	}

	@Test
	void equalsWhenPathIsDifferentReturnsFalse() {
		ConfigTreeConfigDataLocation location = new ConfigTreeConfigDataLocation("/etc/config");
		ConfigTreeConfigDataLocation other = new ConfigTreeConfigDataLocation("other-location");
		assertThat(location).isNotEqualTo(other);
	}

	@Test
	void toStringReturnsDescriptiveString() {
		ConfigTreeConfigDataLocation location = new ConfigTreeConfigDataLocation("/etc/config");
		assertThat(location.toString()).isEqualTo("config tree [" + new File("/etc/config").getAbsolutePath() + "]");
	}

}
