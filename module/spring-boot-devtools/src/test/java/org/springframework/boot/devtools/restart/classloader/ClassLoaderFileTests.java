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

package org.springframework.boot.devtools.restart.classloader;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassLoaderFile}.
 *
 * @author Phillip Webb
 */
class ClassLoaderFileTests {

	public static final byte[] BYTES = "ABC".getBytes();

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void kindMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassLoaderFile(null, null))
			.withMessageContaining("'kind' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addedContentsMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassLoaderFile(Kind.ADDED, null))
			.withMessageContaining("'contents' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void modifiedContentsMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassLoaderFile(Kind.MODIFIED, null))
			.withMessageContaining("'contents' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void deletedContentsMustBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassLoaderFile(Kind.DELETED, new byte[10]))
			.withMessageContaining("'contents' must be null");
	}

	@Test
	void added() {
		ClassLoaderFile file = new ClassLoaderFile(Kind.ADDED, BYTES);
		assertThat(file.getKind()).isEqualTo(ClassLoaderFile.Kind.ADDED);
		assertThat(file.getContents()).isEqualTo(BYTES);
	}

	@Test
	void modified() {
		ClassLoaderFile file = new ClassLoaderFile(Kind.MODIFIED, BYTES);
		assertThat(file.getKind()).isEqualTo(ClassLoaderFile.Kind.MODIFIED);
		assertThat(file.getContents()).isEqualTo(BYTES);
	}

	@Test
	void deleted() {
		ClassLoaderFile file = new ClassLoaderFile(Kind.DELETED, null);
		assertThat(file.getKind()).isEqualTo(ClassLoaderFile.Kind.DELETED);
		assertThat(file.getContents()).isNull();
	}

}
