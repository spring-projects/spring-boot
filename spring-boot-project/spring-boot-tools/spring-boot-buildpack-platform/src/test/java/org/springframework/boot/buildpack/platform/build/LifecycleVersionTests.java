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
	void parseWhenValueIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse(null))
				.withMessage("Value must not be empty");
	}

	@Test
	void parseWhenTooLongThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse("v1.2.3.4"))
				.withMessage("Malformed version number '1.2.3.4'");
	}

	@Test
	void parseWhenNonNumericThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse("v1.2.3a"))
				.withMessage("Malformed version number '1.2.3a'");
	}

	@Test
	void compareTo() {
		LifecycleVersion v4 = LifecycleVersion.parse("0.0.4");
		assertThat(LifecycleVersion.parse("0.0.3").compareTo(v4)).isNegative();
		assertThat(LifecycleVersion.parse("0.0.4").compareTo(v4)).isZero();
		assertThat(LifecycleVersion.parse("0.0.5").compareTo(v4)).isPositive();
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
		assertThat(LifecycleVersion.parse("1.2.3").toString()).isEqualTo("v1.2.3");
		assertThat(LifecycleVersion.parse("1.2").toString()).isEqualTo("v1.2.0");
		assertThat(LifecycleVersion.parse("1").toString()).isEqualTo("v1.0.0");
	}

}
