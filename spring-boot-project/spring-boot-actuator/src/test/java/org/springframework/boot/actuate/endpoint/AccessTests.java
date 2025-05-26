/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Access}.
 *
 * @author Phillip Webb
 */
class AccessTests {

	@Test
	void capWhenAboveMaximum() {
		assertThat(Access.UNRESTRICTED.cap(Access.READ_ONLY)).isEqualTo(Access.READ_ONLY);
	}

	@Test
	void capWhenAtMaximum() {
		assertThat(Access.READ_ONLY.cap(Access.READ_ONLY)).isEqualTo(Access.READ_ONLY);
	}

	@Test
	void capWhenBelowMaximum() {
		assertThat(Access.NONE.cap(Access.READ_ONLY)).isEqualTo(Access.NONE);
	}

}
