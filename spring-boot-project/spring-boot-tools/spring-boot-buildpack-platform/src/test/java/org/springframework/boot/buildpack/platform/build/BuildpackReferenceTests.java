/*
 * Copyright 2012-2022 the original author or authors.
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

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BuildpackReference}.
 *
 * @author Phillip Webb
 */
class BuildpackReferenceTests {

	@Test
	void ofWhenValueIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackReference.of(""))
				.withMessage("Value must not be empty");
	}

	@Test
	void ofCreatesInstance() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference).isNotNull();
	}

	@Test
	void toStringReturnsValue() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference).hasToString("test");
	}

	@Test
	void equalsAndHashCode() {
		BuildpackReference a = BuildpackReference.of("test1");
		BuildpackReference b = BuildpackReference.of("test1");
		BuildpackReference c = BuildpackReference.of("test2");
		assertThat(a).isEqualTo(a).isEqualTo(b).isNotEqualTo(c);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	@Test
	void hasPrefixWhenPrefixMatchReturnsTrue() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference.hasPrefix("te")).isTrue();
	}

	@Test
	void hasPrefixWhenPrefixMismatchReturnsFalse() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference.hasPrefix("st")).isFalse();
	}

	@Test
	void getSubReferenceWhenPrefixMatchReturnsSubReference() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference.getSubReference("te")).isEqualTo("st");
	}

	@Test
	void getSubReferenceWhenPrefixMismatchReturnsNull() {
		BuildpackReference reference = BuildpackReference.of("test");
		assertThat(reference.getSubReference("st")).isNull();
	}

	@Test
	void asPathWhenFileUrlReturnsPath() {
		BuildpackReference reference = BuildpackReference.of("file:///test.dat");
		assertThat(reference.asPath()).isEqualTo(Paths.get("/test.dat"));
	}

	@Test
	void asPathWhenPathReturnsPath() {
		BuildpackReference reference = BuildpackReference.of("/test.dat");
		assertThat(reference.asPath()).isEqualTo(Paths.get("/test.dat"));
	}

}
