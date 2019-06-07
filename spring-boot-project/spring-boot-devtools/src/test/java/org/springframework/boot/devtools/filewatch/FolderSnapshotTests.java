/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FolderSnapshot}.
 *
 * @author Phillip Webb
 */
public class FolderSnapshotTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File folder;

	private FolderSnapshot initialSnapshot;

	@Before
	public void setup() throws Exception {
		this.folder = createTestFolderStructure();
		this.initialSnapshot = new FolderSnapshot(this.folder);
	}

	@Test
	public void folderMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Folder must not be null");
		new FolderSnapshot(null);
	}

	@Test
	public void folderMustNotBeFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Folder '" + file + "' must not be a file");
		new FolderSnapshot(file);
	}

	@Test
	public void folderDoesNotHaveToExist() throws Exception {
		File file = new File(this.temporaryFolder.getRoot(), "does/not/exist");
		FolderSnapshot snapshot = new FolderSnapshot(file);
		assertThat(snapshot).isEqualTo(new FolderSnapshot(file));
	}

	@Test
	public void equalsWhenNothingHasChanged() {
		FolderSnapshot updatedSnapshot = new FolderSnapshot(this.folder);
		assertThat(this.initialSnapshot).isEqualTo(updatedSnapshot);
		assertThat(this.initialSnapshot.hashCode()).isEqualTo(updatedSnapshot.hashCode());
	}

	@Test
	public void notEqualsWhenAFileIsAdded() throws Exception {
		new File(new File(this.folder, "folder1"), "newfile").createNewFile();
		FolderSnapshot updatedSnapshot = new FolderSnapshot(this.folder);
		assertThat(this.initialSnapshot).isNotEqualTo(updatedSnapshot);
	}

	@Test
	public void notEqualsWhenAFileIsDeleted() {
		new File(new File(this.folder, "folder1"), "file1").delete();
		FolderSnapshot updatedSnapshot = new FolderSnapshot(this.folder);
		assertThat(this.initialSnapshot).isNotEqualTo(updatedSnapshot);
	}

	@Test
	public void notEqualsWhenAFileIsModified() throws Exception {
		File file1 = new File(new File(this.folder, "folder1"), "file1");
		FileCopyUtils.copy("updatedcontent".getBytes(), file1);
		FolderSnapshot updatedSnapshot = new FolderSnapshot(this.folder);
		assertThat(this.initialSnapshot).isNotEqualTo(updatedSnapshot);
	}

	@Test
	public void getChangedFilesSnapshotMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Snapshot must not be null");
		this.initialSnapshot.getChangedFiles(null, null);
	}

	@Test
	public void getChangedFilesSnapshotMustBeTheSameSourceFolder() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Snapshot source folder must be '" + this.folder + "'");
		this.initialSnapshot.getChangedFiles(new FolderSnapshot(createTestFolderStructure()), null);
	}

	@Test
	public void getChangedFilesWhenNothingHasChanged() {
		FolderSnapshot updatedSnapshot = new FolderSnapshot(this.folder);
		this.initialSnapshot.getChangedFiles(updatedSnapshot, null);
	}

	@Test
	public void getChangedFilesWhenAFileIsAddedAndDeletedAndChanged() throws Exception {
		File folder1 = new File(this.folder, "folder1");
		File file1 = new File(folder1, "file1");
		File file2 = new File(folder1, "file2");
		File newFile = new File(folder1, "newfile");
		FileCopyUtils.copy("updatedcontent".getBytes(), file1);
		file2.delete();
		newFile.createNewFile();
		FolderSnapshot updatedSnapshot = new FolderSnapshot(this.folder);
		ChangedFiles changedFiles = this.initialSnapshot.getChangedFiles(updatedSnapshot, null);
		assertThat(changedFiles.getSourceFolder()).isEqualTo(this.folder);
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

	private File createTestFolderStructure() throws IOException {
		File root = this.temporaryFolder.newFolder();
		File folder1 = new File(root, "folder1");
		folder1.mkdirs();
		FileCopyUtils.copy("abc".getBytes(), new File(folder1, "file1"));
		FileCopyUtils.copy("abc".getBytes(), new File(folder1, "file2"));
		return root;
	}

}
