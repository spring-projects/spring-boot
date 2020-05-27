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

package org.springframework.boot.loader.tools.layer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ApplicationContentFilter}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ApplicationContentFilterTests {

	@Test
	void createWhenPatternIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationContentFilter(null))
				.withMessage("Pattern must not be empty");
	}

	@Test
	void createWhenPatternIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationContentFilter(""))
				.withMessage("Pattern must not be empty");
	}

	@Test
	void matchesWhenWildcardPatternMatchesReturnsTrue() {
		ApplicationContentFilter filter = new ApplicationContentFilter("META-INF/**");
		assertThat(filter.matches("META-INF/resources/application.yml")).isTrue();
	}

	@Test
	void matchesWhenWildcardPatternDoesNotMatchReturnsFalse() {
		ApplicationContentFilter filter = new ApplicationContentFilter("META-INF/**");
		assertThat(filter.matches("src/main/resources/application.yml")).isFalse();
	}

}
