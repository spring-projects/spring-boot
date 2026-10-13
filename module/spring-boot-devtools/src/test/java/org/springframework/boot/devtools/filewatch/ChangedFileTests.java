/*
 * Copyright 2012-present the original author or authors.
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
	@SuppressWarnings("NullAway") // Test null check
	void sourceDirectoryMustNotBeNull() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ChangedFile(null, new File(this.tempDir, "file"), Type.ADD))
			.withMessageContaining("'sourceDirectory' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void fileMustNotBeNull() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ChangedFile(new File(this.tempDir, "directory"), null, Type.ADD))
			.withMessageContaining("'file' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void typeMustNotBeNull() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new ChangedFile(new File(this.tempDir, "file"), new File(this.tempDir, "directory"), null))
			.withMessageContaining("'type' must not be null");
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
