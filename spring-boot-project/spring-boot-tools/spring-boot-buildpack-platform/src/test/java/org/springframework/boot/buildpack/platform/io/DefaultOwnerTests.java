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

package org.springframework.boot.buildpack.platform.io;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultOwner}.
 *
 * @author Phillip Webb
 */
class DefaultOwnerTests {

	@Test
	void getUidReturnsUid() {
		DefaultOwner owner = new DefaultOwner(123, 456);
		assertThat(owner.getUid()).isEqualTo(123);
	}

	@Test
	void getGidReturnsGid() {
		DefaultOwner owner = new DefaultOwner(123, 456);
		assertThat(owner.getGid()).isEqualTo(456);
	}

	@Test
	void toStringReturnsString() {
		DefaultOwner owner = new DefaultOwner(123, 456);
		assertThat(owner).hasToString("123/456");
	}

}
