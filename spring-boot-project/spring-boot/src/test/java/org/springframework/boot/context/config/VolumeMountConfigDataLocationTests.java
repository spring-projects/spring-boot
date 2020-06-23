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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link VolumeMountConfigDataLocation}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class VolumeMountConfigDataLocationTests {

	@Test
	void constructorWhenPathIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new VolumeMountConfigDataLocation(null))
				.withMessage("Path must not be null");
	}

	@Test
	void equalsWhenPathIsTheSameReturnsTrue() {
		VolumeMountConfigDataLocation location = new VolumeMountConfigDataLocation("/etc/config");
		VolumeMountConfigDataLocation other = new VolumeMountConfigDataLocation("/etc/config");
		assertThat(location).isEqualTo(other);
	}

	@Test
	void equalsWhenPathIsDifferentReturnsFalse() {
		VolumeMountConfigDataLocation location = new VolumeMountConfigDataLocation("/etc/config");
		VolumeMountConfigDataLocation other = new VolumeMountConfigDataLocation("other-location");
		assertThat(location).isNotEqualTo(other);
	}

	@Test
	void toStringReturnsDescriptiveString() {
		VolumeMountConfigDataLocation location = new VolumeMountConfigDataLocation("/etc/config");
		assertThat(location.toString()).isEqualTo("volume mount [/etc/config]");
	}

}
