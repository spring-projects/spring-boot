/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.filewatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
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
	@SuppressWarnings("NullAway") // Test null check
	void directoryMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectorySnapshot(null))
			.withMessageContaining("'directory' must not be null");
	}

	@Test
	void directoryMustNotBeFile() throws Exception {
		File file = new File(this.tempDir, "file");
		file.createNewFile();
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectorySnapshot(file))
			.withMessageContaining("'directory' [" + file + "] must not be a file");
	}

	@Test
	void directoryDoesNotHaveToExist() {
		File file = new File(this.tempDir, "does/not/exist");
		DirectorySnapshot snapshot = new DirectorySnapshot(file);
		assertThat(snapshot).isEqualTo(new DirectorySnapshot(file));
	}

	@Test
	void equalsWhenNothingHasChanged() {
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		assertThat(this.initialSnapshot).isEqualTo(updatedSnapshot);
		assertThat(this.initialSnapshot).hasSameHashCodeAs(updatedSnapshot);
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
	@SuppressWarnings("NullAway") // Test null check
	void getChangedFilesSnapshotMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.initialSnapshot.getChangedFiles(null, null))
			.withMessageContaining("'snapshot' must not be null");
	}

	@Test
	void getChangedFilesSnapshotMustBeTheSameSourceDirectory() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.initialSnapshot.getChangedFiles(new DirectorySnapshot(createTestDirectoryStructure()), null))
			.withMessageContaining("'snapshot' source directory must be '" + this.directory + "'");
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

	@Test
	void getChangedFilesWhenAFileIsChangedThroughASymbolicLink() throws Exception {
		File target = createTestDirectoryStructure();
		File directoryLink = new File(this.directory, "directory-link");
		File fileLink = new File(this.directory, "file-link");
		createSymbolicLink(directoryLink, target);
		createSymbolicLink(fileLink, new File(new File(target, "directory1"), "file2"));
		DirectorySnapshot initialSnapshot = new DirectorySnapshot(this.directory);
		File linkedFile1 = new File(new File(directoryLink, "directory1"), "file1");
		FileCopyUtils.copy("updatedcontent".getBytes(), linkedFile1);
		FileCopyUtils.copy("updatedcontent".getBytes(), fileLink);
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		ChangedFiles changedFiles = initialSnapshot.getChangedFiles(updatedSnapshot, null);
		assertThat(changedFiles.getFiles()).hasSize(3);
		assertThat(getChangedFile(changedFiles, linkedFile1).getType()).isEqualTo(Type.MODIFY);
		assertThat(getChangedFile(changedFiles, new File(new File(directoryLink, "directory1"), "file2")).getType())
			.isEqualTo(Type.MODIFY);
		assertThat(getChangedFile(changedFiles, fileLink).getType()).isEqualTo(Type.MODIFY);
	}

	@Test
	void brokenSymbolicLinkIsIgnored() {
		createSymbolicLink(new File(this.directory, "broken-link"), new File(this.tempDir, "does-not-exist"));
		DirectorySnapshot updatedSnapshot = new DirectorySnapshot(this.directory);
		assertThat(this.initialSnapshot).isEqualTo(updatedSnapshot);
	}

	private void createSymbolicLink(File link, File target) {
		try {
			Files.createSymbolicLink(link.toPath(), target.toPath());
		}
		catch (Exception ex) {
			Assumptions.abort("Symlink creation not supported");
		}
	}

	private ChangedFile getChangedFile(ChangedFiles changedFiles, File file) {
		for (ChangedFile changedFile : changedFiles) {
			if (changedFile.getFile().equals(file)) {
				return changedFile;
			}
		}
		throw new AssertionError("File '%s' not found".formatted(file));
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
