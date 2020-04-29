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

package org.springframework.boot.devtools.filewatch;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DirectorySnapshot}.
 *
 * @author Phillip Webb
 */
class DirectorySnapshotTests {

	@TempDir
	File tempDir;

	private File directory;

	private DirectorySnapshot initialSnapshot;

	@BeforeEach
	void setup() throws Exception {
		this.directory = createTestDirectoryStructure();
		this.initialSnapshot = new DirectorySnapshot(this.directory);
	}

	@Test
	void directoryMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectorySnapshot(null))
				.withMessageContaining("Directory must not be null");
	}

	@Test
	void directoryMustNotBeFile() throws Exception {
		File file = new File(this.tempDir, "file");
		file.createNewFile();
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectorySnapshot(file))
				.withMessageContaining("Directory '" + file + "' must not be a file");
	}

	@Test
	void directoryDoesNotHaveToExist() throws Exception {
		File file = new File(this.tempDir, "does/not/exist");
		DirectorySnapshot snapshot = new DirectorySnapshot(file);
		assertThat(snapshot).isEqualTo(new DirectorySnapshot(file));
	}

	@Test
	void equalsWhenNothingHasChanged() {
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		assertThat(this.initialSnapshot).isEqualTo(updatedSnapshot);
		assertThat(this.initialSnapshot.hashCode()).isEqualTo(updatedSnapshot.hashCode());
	}

	@Test
	void notEqualsWhenAFileIsAdded() throws Exception {
		new File(new File(this.directory, "directory1"), "newfile").createNewFile();
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		assertThat(this.initialSnapshot).isNotEqualTo(updatedSnapshot);
	}

	@Test
	void notEqualsWhenAFileIsDeleted() {
		new File(new File(this.directory, "directory1"), "file1").delete();
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		assertThat(this.initialSnapshot).isNotEqualTo(updatedSnapshot);
	}

	@Test
	void notEqualsWhenAFileIsModified() throws Exception {
		File file1 = new File(new File(this.directory, "directory1"), "file1");
		FileCopyUtils.copy("updatedcontent".getBytes(), file1);
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		assertThat(this.initialSnapshot).isNotEqualTo(updatedSnapshot);
	}

	@Test
	void getChangedFilesSnapshotMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.initialSnapshot.getChangedFiles(null, null))
				.withMessageContaining("Snapshot must not be null");
	}

	@Test
	void getChangedFilesSnapshotMustBeTheSameSourceDirectory() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.initialSnapshot.getChangedFiles(new DirectorySnapshot(createTestDirectoryStructure()), null))
				.withMessageContaining("Snapshot source directory must be '" + this.directory + "'");
	}

	@Test
	void getChangedFilesWhenNothingHasChanged() {
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		this.initialSnapshot.getChangedFiles(updatedSnapshot, null);
	}

	@Test
	void getChangedFilesWhenAFileIsAddedAndDeletedAndChanged() throws Exception {
		File directory1 = new File(this.directory, "directory1");
		File file1 = new File(directory1, "file1");
		File file2 = new File(directory1, "file2");
		File newFile = new File(directory1, "newfile");
		FileCopyUtils.copy("updatedcontent".getBytes(), file1);
		file2.delete();
		newFile.createNewFile();
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		ChangedFiles changedFiles = this.initialSnapshot.getChangedFiles(updatedSnapshot, null);
		assertThat(changedFiles.getSourceDirectory()).isEqualTo(this.directory);
		assertThat(getChangedFile(changedFiles, file1).getType()).isEqualTo(Type.MODIFY);
		assertThat(getChangedFile(changedFiles, file2).getType()).isEqualTo(Type.DELETE);
		assertThat(getChangedFile(changedFiles, newFile).getType()).isEqualTo(Type.ADD);
	}

	private ChangedFile getChangedFile(ChangedFiles changedFiles, File file) {
		for (ChangedFile changedFile : changedFiles) {
			if (changedFile.getFile().equals(file)) {
				return changedFile;
			}
		}
		return null;
	}

	private File createTestDirectoryStructure() throws IOException {
		File root = new File(this.tempDir, UUID.randomUUID().toString());
		File directory1 = new File(root, "directory1");
		directory1.mkdirs();
		FileCopyUtils.copy("abc".getBytes(), new File(directory1, "file1"));
		FileCopyUtils.copy("abc".getBytes(), new File(directory1, "file2"));
		return root;
	}

}
