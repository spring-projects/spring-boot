/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.devtools.filewatch;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.devtools.filewatch.ChangedFile.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ChangedFile}.
 *
 * @author Phillip Webb
 */
class ChangedFileTests {

	@TempDir
	File tempDir;

	@Test
	void sourceDirectoryMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ChangedFile(null, new File(this.tempDir, "file"), Type.ADD))
				.withMessageContaining("SourceDirectory must not be null");
	}

	@Test
	void fileMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ChangedFile(new File(this.tempDir, "directory"), null, Type.ADD))
				.withMessageContaining("File must not be null");
	}

	@Test
	void typeMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new ChangedFile(new File(this.tempDir, "file"), new File(this.tempDir, "directory"), null))
				.withMessageContaining("Type must not be null");
	}

	@Test
	void getFile() {
		File file = new File(this.tempDir, "file");
		ChangedFile changedFile = new ChangedFile(new File(this.tempDir, "directory"), file, Type.ADD);
		assertThat(changedFile.getFile()).isEqualTo(file);
	}

	@Test
	void getType() {
		ChangedFile changedFile = new ChangedFile(new File(this.tempDir, "directory"), new File(this.tempDir, "file"),
				Type.DELETE);
		assertThat(changedFile.getType()).isEqualTo(Type.DELETE);
	}

	@Test
	void getRelativeName() {
		File subDirectory = new File(this.tempDir, "A");
		File file = new File(subDirectory, "B.txt");
		ChangedFile changedFile = new ChangedFile(this.tempDir, file, Type.ADD);
		assertThat(changedFile.getRelativeName()).isEqualTo("A/B.txt");
	}

}
