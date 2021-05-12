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

package org.springframework.boot.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ContainerReference}.
 *
 * @author Phillip Webb
 */
class ContainerReferenceTests {

	@Test
	void ofCreatesInstance() {
		ContainerReference reference = ContainerReference
				.of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
		assertThat(reference.toString()).isEqualTo("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
	}

	@Test
	void ofWhenNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerReference.of(null))
				.withMessage("Value must not be empty");
	}

	@Test
	void ofWhenEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerReference.of(""))
				.withMessage("Value must not be empty");
	}

	@Test
	void hashCodeAndEquals() {
		ContainerReference r1 = ContainerReference
				.of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
		ContainerReference r2 = ContainerReference
				.of("92691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
		ContainerReference r3 = ContainerReference
				.of("02691aec176333f7ae890de9aaeeafef11166efcaa3908edf83eb44a5c943781");
		assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
		assertThat(r1).isEqualTo(r1).isEqualTo(r2).isNotEqualTo(r3);
	}

}
